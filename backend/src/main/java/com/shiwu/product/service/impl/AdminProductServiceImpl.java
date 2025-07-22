package com.shiwu.product.service.impl;

import com.shiwu.admin.enums.AuditActionEnum;
import com.shiwu.admin.enums.AuditTargetTypeEnum;
import com.shiwu.admin.model.AdminProductQueryDTO;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.admin.service.AuditLogService;
import com.shiwu.admin.service.impl.AuditLogServiceImpl;
import com.shiwu.product.dao.AdminProductDao;
import com.shiwu.product.dao.ProductDao;
import com.shiwu.product.model.Product;
import com.shiwu.product.service.AdminProductService;
import com.shiwu.notification.service.NotificationService;
import com.shiwu.notification.service.impl.NotificationServiceImpl;
import com.shiwu.common.result.Result;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员商品服务实现类 - MVC框架版本
 * 实现NFR-SEC-03要求，在所有敏感操作中嵌入审计日志记录
 */
@Service
public class AdminProductServiceImpl extends BaseService implements AdminProductService {

    @Autowired
    private AdminProductDao adminProductDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    public AdminProductServiceImpl() {
        this.adminProductDao = new AdminProductDao();
        this.productDao = new ProductDao();
        this.auditLogService = new AuditLogServiceImpl();
        this.notificationService = new NotificationServiceImpl();
    }

    // 用于测试的构造函数
    public AdminProductServiceImpl(AdminProductDao adminProductDao, ProductDao productDao,
                                 AuditLogService auditLogService, NotificationService notificationService) {
        this.adminProductDao = adminProductDao;
        this.productDao = productDao;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Override
    public Map<String, Object> findProducts(AdminProductQueryDTO queryDTO) {
        if (queryDTO == null) {
            logger.warn("查询商品列表失败: 查询条件为空");
            return null;
        }

        try {
            // 查询商品列表
            List<Map<String, Object>> products = adminProductDao.findProducts(queryDTO);

            // 查询总数
            long totalCount = adminProductDao.countProducts(queryDTO);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("products", products);
            result.put("totalCount", totalCount);
            result.put("page", queryDTO.getPageNum());
            result.put("pageSize", queryDTO.getPageSize());
            result.put("totalPages", (totalCount + queryDTO.getPageSize() - 1) / queryDTO.getPageSize());
            
            logger.info("查询商品列表成功: 共{}条记录", totalCount);
            return result;
        } catch (Exception e) {
            logger.error("查询商品列表失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getProductDetail(Long productId, Long adminId) {
        if (productId == null || adminId == null) {
            logger.warn("获取商品详情失败: 参数为空");
            return null;
        }

        try {
            // 管理员可以查看所有商品详情（包括被删除的商品）
            Product product = productDao.findById(productId);
            
            if (product == null) {
                logger.warn("获取商品详情失败: 商品不存在, productId={}", productId);
                return null;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("product", product);
            
            logger.info("管理员 {} 获取商品详情成功: productId={}", adminId, productId);
            return result;
        } catch (Exception e) {
            logger.error("获取商品详情失败: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean approveProduct(Long productId, Long adminId, String reason, String ipAddress, String userAgent) {
        if (productId == null || adminId == null) {
            logger.warn("审核通过商品失败: 参数为空");
            return false;
        }

        try {
            // 检查商品是否存在
            Product product = productDao.findById(productId);
            if (product == null) {
                logger.warn("审核通过商品失败: 商品不存在, productId={}", productId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_APPROVE, AuditTargetTypeEnum.PRODUCT, 
                                         productId, "审核通过商品失败: 商品不存在" + (reason != null ? ", 备注: " + reason : ""), 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 检查商品当前状态
            if (!Product.STATUS_PENDING_REVIEW.equals(product.getStatus())) {
                logger.warn("审核通过商品失败: 商品状态不是待审核, productId={}, status={}", productId, product.getStatus());
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_APPROVE, AuditTargetTypeEnum.PRODUCT, 
                                         productId, "审核通过商品失败: 商品状态不是待审核" + (reason != null ? ", 备注: " + reason : ""), 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 更新商品状态为上架
            boolean success = adminProductDao.updateProductStatus(productId, Product.STATUS_ONSALE, adminId);
            
            // 记录审计日志
            String details = "审核通过商品: " + product.getTitle() + " (ID: " + productId + ")" + 
                           (reason != null ? ", 备注: " + reason : "");
            auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_APPROVE, AuditTargetTypeEnum.PRODUCT, 
                                     productId, details, ipAddress, userAgent, success);
            
            if (success) {
                logger.info("管理员 {} 审核通过商品 {} 成功", adminId, productId);

                // Task4_2_1_2: 商品首次审核通过时，为卖家的所有粉丝生成动态通知
                try {
                    Result<Integer> notificationResult = notificationService.createProductApprovedNotifications(
                        productId, product.getSellerId(), product.getTitle());

                    if (notificationResult.isSuccess()) {
                        int notificationCount = notificationResult.getData();
                        logger.info("为商品审核通过创建粉丝通知成功: productId={}, sellerId={}, notificationCount={}",
                                   productId, product.getSellerId(), notificationCount);
                    } else {
                        logger.warn("为商品审核通过创建粉丝通知失败: productId={}, error={}",
                                   productId, notificationResult.getMessage());
                    }
                } catch (Exception notificationEx) {
                    // 通知创建失败不影响审核通过的主流程
                    logger.error("创建商品审核通过通知时发生异常: productId={}, error={}",
                               productId, notificationEx.getMessage(), notificationEx);
                }

                return true;
            } else {
                logger.warn("审核通过商品失败: 更新状态失败, productId={}", productId);
                return false;
            }
        } catch (Exception e) {
            logger.error("审核通过商品失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_APPROVE, AuditTargetTypeEnum.PRODUCT, 
                                     productId, "审核通过商品异常: " + e.getMessage() + (reason != null ? ", 备注: " + reason : ""), 
                                     ipAddress, userAgent, false);
            return false;
        }
    }

    @Override
    public boolean rejectProduct(Long productId, Long adminId, String reason, String ipAddress, String userAgent) {
        if (productId == null || adminId == null) {
            logger.warn("审核拒绝商品失败: 参数为空");
            return false;
        }

        if (reason == null || reason.trim().isEmpty()) {
            logger.warn("审核拒绝商品失败: 拒绝原因不能为空");
            return false;
        }

        try {
            // 检查商品是否存在
            Product product = productDao.findById(productId);
            if (product == null) {
                logger.warn("审核拒绝商品失败: 商品不存在, productId={}", productId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_REJECT, AuditTargetTypeEnum.PRODUCT, 
                                         productId, "审核拒绝商品失败: 商品不存在, 原因: " + reason, 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 检查商品当前状态
            if (!Product.STATUS_PENDING_REVIEW.equals(product.getStatus())) {
                logger.warn("审核拒绝商品失败: 商品状态不是待审核, productId={}, status={}", productId, product.getStatus());
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_REJECT, AuditTargetTypeEnum.PRODUCT, 
                                         productId, "审核拒绝商品失败: 商品状态不是待审核, 原因: " + reason, 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 更新商品状态为草稿
            boolean success = adminProductDao.updateProductStatus(productId, Product.STATUS_DRAFT, adminId);
            
            // 记录审计日志
            String details = "审核拒绝商品: " + product.getTitle() + " (ID: " + productId + "), 原因: " + reason;
            auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_REJECT, AuditTargetTypeEnum.PRODUCT, 
                                     productId, details, ipAddress, userAgent, success);
            
            if (success) {
                logger.info("管理员 {} 审核拒绝商品 {} 成功, 原因: {}", adminId, productId, reason);
                return true;
            } else {
                logger.warn("审核拒绝商品失败: 更新状态失败, productId={}", productId);
                return false;
            }
        } catch (Exception e) {
            logger.error("审核拒绝商品失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_REJECT, AuditTargetTypeEnum.PRODUCT, 
                                     productId, "审核拒绝商品异常: " + e.getMessage() + ", 原因: " + reason, 
                                     ipAddress, userAgent, false);
            return false;
        }
    }

    @Override
    public boolean delistProduct(Long productId, Long adminId, String reason, String ipAddress, String userAgent) {
        if (productId == null || adminId == null) {
            logger.warn("下架商品失败: 参数为空");
            return false;
        }

        try {
            // 检查商品是否存在
            Product product = productDao.findById(productId);
            if (product == null) {
                logger.warn("下架商品失败: 商品不存在, productId={}", productId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_TAKEDOWN, AuditTargetTypeEnum.PRODUCT, 
                                         productId, "下架商品失败: 商品不存在" + (reason != null ? ", 原因: " + reason : ""), 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 检查商品当前状态
            if (!Product.STATUS_ONSALE.equals(product.getStatus())) {
                logger.warn("下架商品失败: 商品状态不是在售, productId={}, status={}", productId, product.getStatus());
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_TAKEDOWN, AuditTargetTypeEnum.PRODUCT, 
                                         productId, "下架商品失败: 商品状态不是在售" + (reason != null ? ", 原因: " + reason : ""), 
                                         ipAddress, userAgent, false);
                return false;
            }

            // 更新商品状态为下架
            boolean success = adminProductDao.updateProductStatus(productId, Product.STATUS_DELISTED, adminId);
            
            // 记录审计日志
            String details = "下架商品: " + product.getTitle() + " (ID: " + productId + ")" + 
                           (reason != null ? ", 原因: " + reason : "");
            auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_TAKEDOWN, AuditTargetTypeEnum.PRODUCT, 
                                     productId, details, ipAddress, userAgent, success);
            
            if (success) {
                logger.info("管理员 {} 下架商品 {} 成功", adminId, productId);
                return true;
            } else {
                logger.warn("下架商品失败: 更新状态失败, productId={}", productId);
                return false;
            }
        } catch (Exception e) {
            logger.error("下架商品失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_TAKEDOWN, AuditTargetTypeEnum.PRODUCT, 
                                     productId, "下架商品异常: " + e.getMessage() + (reason != null ? ", 原因: " + reason : ""), 
                                     ipAddress, userAgent, false);
            return false;
        }
    }

    @Override
    public boolean deleteProduct(Long productId, Long adminId, String ipAddress, String userAgent) {
        if (productId == null || adminId == null) {
            logger.warn("删除商品失败: 参数为空");
            return false;
        }

        try {
            // 检查商品是否存在
            Product product = productDao.findById(productId);
            if (product == null) {
                logger.warn("删除商品失败: 商品不存在, productId={}", productId);
                // 记录失败的审计日志
                auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_DELETE, AuditTargetTypeEnum.PRODUCT, 
                                         productId, "删除商品失败: 商品不存在", ipAddress, userAgent, false);
                return false;
            }

            // 软删除商品
            boolean success = adminProductDao.deleteProduct(productId, adminId);
            
            // 记录审计日志
            String details = "删除商品: " + product.getTitle() + " (ID: " + productId + ")";
            auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_DELETE, AuditTargetTypeEnum.PRODUCT, 
                                     productId, details, ipAddress, userAgent, success);

            if (success) {
                logger.info("管理员 {} 删除商品 {} 成功", adminId, productId);
                return true;
            } else {
                logger.warn("删除商品失败: 删除操作失败, productId={}", productId);
                return false;
            }
        } catch (Exception e) {
            logger.error("删除商品失败: {}", e.getMessage(), e);
            // 记录异常的审计日志
            auditLogService.logAction(adminId, AuditActionEnum.PRODUCT_DELETE, AuditTargetTypeEnum.PRODUCT, 
                                     productId, "删除商品异常: " + e.getMessage(), ipAddress, userAgent, false);
            return false;
        }
    }
}
