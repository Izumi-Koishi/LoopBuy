package com.shiwu.product.service.impl;

import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.product.dao.CategoryDao;
import com.shiwu.product.dao.ProductDao;
import com.shiwu.product.model.*;
import com.shiwu.product.service.ProductService;


//import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 商品服务实现类 - MVC框架版本
 */
@Service
public class ProductServiceImpl extends BaseService implements ProductService {
    
    // 文件上传配置
    private static final String UPLOAD_DIR = "uploads/products";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );
    
    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private ProductDao productDao;

    public ProductServiceImpl() {
        // 为了兼容测试，在无参构造函数中初始化DAO
        this.categoryDao = new CategoryDao();
        this.productDao = new ProductDao();
        logger.info("ProductServiceImpl初始化完成 - 使用MVC框架依赖注入");

        // 确保上传目录存在
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("创建商品图片上传目录: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("创建上传目录失败: {}", e.getMessage(), e);
        }
    }

    // 兼容性构造函数，支持渐进式迁移
    public ProductServiceImpl(CategoryDao categoryDao, ProductDao productDao) {
        this.categoryDao = categoryDao;
        this.productDao = productDao;
        logger.info("ProductServiceImpl初始化完成 - 使用兼容性构造函数");
        
        // 确保上传目录存在
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("创建商品图片上传目录: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("创建上传目录失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public List<CategoryVO> getAllCategories() {
        return categoryDao.findAll();
    }
    
    @Override
    public Long createProduct(ProductCreateDTO dto, Long sellerId) {
        logMethodStart("createProduct", dto != null ? dto.getTitle() : null, sellerId);

        try {
            // 参数校验
            validateNotNull(dto, "dto");
            validateId(sellerId, "sellerId");
            validateNotBlank(dto.getTitle(), "title");
        } catch (IllegalArgumentException e) {
            logBusinessWarning("创建商品失败: {}", e.getMessage());
            return null;
        }
        
        // 如果是提交审核，则进行更严格的校验
        if (ProductCreateDTO.ACTION_SUBMIT_REVIEW.equals(dto.getAction())) {
            try {
                // 检查描述是否为空
                validateNotBlank(dto.getDescription(), "description");

                // 检查价格是否合法
                validateNotNull(dto.getPrice(), "price");
                validatePositive(dto.getPrice(), "price");
            } catch (IllegalArgumentException e) {
                logBusinessWarning("提交商品审核失败: {}", e.getMessage());
                return null;
            }
            
            // 检查分类是否存在
            if (dto.getCategoryId() == null) {
                logger.warn("提交商品审核失败: 未选择商品分类");
                return null;
            }
            
            Category category = categoryDao.findById(dto.getCategoryId());
            if (category == null) {
                logger.warn("提交商品审核失败: 商品分类不存在, categoryId={}", dto.getCategoryId());
                return null;
            }
        }
        
        // 创建商品对象
        Product product = new Product();
        product.setSellerId(sellerId);
        product.setTitle(dto.getTitle());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setCategoryId(dto.getCategoryId());
        
        // 设置状态: 如果是提交审核，则状态为待审核；否则为草稿
        if (ProductCreateDTO.ACTION_SUBMIT_REVIEW.equals(dto.getAction())) {
            product.setStatus(Product.STATUS_PENDING_REVIEW);
        } else {
            // 保存为草稿状态
            product.setStatus(Product.STATUS_DRAFT);
        }
        
        // 保存商品
        return productDao.createProduct(product);
    }
    
    @Override
    public String uploadProductImage(Long productId, String imageName, InputStream imageInputStream, 
                                   String contentType, Boolean isMain, Long sellerId) {
        // 参数校验
        if (productId == null || imageInputStream == null || contentType == null || sellerId == null) {
            logger.warn("上传商品图片失败: 参数为空");
            return null;
        }
        
        // 验证文件类型
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            logger.warn("上传商品图片失败: 不支持的文件类型 {}", contentType);
            return null;
        }
        
        // 检查商品是否属于该卖家
        if (!isProductOwnedBySeller(productId, sellerId)) {
            logger.warn("上传商品图片失败: 商品不属于该卖家, productId={}, sellerId={}", productId, sellerId);
            return null;
        }
        
        try {
            // 检查文件大小
            if (imageInputStream.available() > MAX_FILE_SIZE) {
                logger.warn("上传商品图片失败: 文件大小超过限制 {}MB", MAX_FILE_SIZE / (1024 * 1024));
                return null;
            }
            
            // 生成唯一文件名
            String fileExtension = getFileExtension(contentType);
            String fileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            
            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = imageInputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            // 生成访问URL
            String imageUrl = "/uploads/products/" + fileName;
            
            // 保存到数据库
            ProductImage productImage = new ProductImage();
            productImage.setProductId(productId);
            productImage.setImageUrl(imageUrl);
            productImage.setIsMain(isMain != null && isMain);
            
            if (productDao.addProductImage(productImage)) {
                logger.info("商品图片上传成功: productId={}, imageUrl={}", productId, imageUrl);
                return imageUrl;
            } else {
                // 如果数据库保存失败，删除已上传的文件
                Files.deleteIfExists(filePath);
                logger.error("商品图片数据库保存失败");
                return null;
            }
        } catch (IOException e) {
            logger.error("上传商品图片失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public boolean isProductOwnedBySeller(Long productId, Long sellerId) {
        if (productId == null || sellerId == null) {
            return false;
        }
        
        Product product = productDao.findById(productId);
        return product != null && product.getSellerId().equals(sellerId);
    }
    
    @Override
    public List<ProductCardVO> getProductsBySellerIdAndStatus(Long sellerId, Integer status) {
        if (sellerId == null) {
            logger.warn("查询卖家商品列表失败: 卖家ID为空");
            return Collections.emptyList();
        }
        
        return productDao.findProductsBySellerIdAndStatus(sellerId, status);
    }
    
    @Override
    public Product getProductById(Long productId) {
        if (productId == null) {
            logger.warn("查询商品详情失败: 商品ID为空");
            return null;
        }
        
        return productDao.findById(productId);
    }
    
    @Override
    public boolean updateProduct(Product product, Long sellerId) {
        if (product == null || product.getId() == null || sellerId == null) {
            logger.warn("更新商品失败: 参数为空");
            return false;
        }
        
        // 检查商品是否属于该卖家
        if (!isProductOwnedBySeller(product.getId(), sellerId)) {
            logger.warn("更新商品失败: 商品不属于该卖家, productId={}, sellerId={}", product.getId(), sellerId);
            return false;
        }
        
        // 检查必填字段
        if (product.getTitle() == null || product.getTitle().trim().isEmpty()) {
            logger.warn("更新商品失败: 商品标题为空");
            return false;
        }
        
        // 获取原商品信息，仅更新允许修改的字段
        Product original = productDao.findById(product.getId());
        if (original == null) {
            logger.warn("更新商品失败: 商品不存在, productId={}", product.getId());
            return false;
        }
        
        // 保留原有的不可修改字段
        product.setSellerId(original.getSellerId());
        product.setStatus(original.getStatus());
        product.setCreateTime(original.getCreateTime());
        
        return productDao.updateProduct(product);
    }
    
    @Override
    public boolean updateProductStatus(Long productId, Integer status, Long sellerId) {
        if (productId == null || status == null || sellerId == null) {
            logger.warn("更新商品状态失败: 参数为空");
            return false;
        }
        
        // 检查商品是否属于该卖家
        if (!isProductOwnedBySeller(productId, sellerId)) {
            logger.warn("更新商品状态失败: 商品不属于该卖家, productId={}, sellerId={}", productId, sellerId);
            return false;
        }
        
        // 检查状态值是否合法
        if (status != Product.STATUS_ONSALE && 
            status != Product.STATUS_DELISTED && 
            status != Product.STATUS_DRAFT && 
            status != Product.STATUS_PENDING_REVIEW) {
            logger.warn("更新商品状态失败: 状态值非法, status={}", status);
            return false;
        }
        
        // 获取商品当前状态
        Product product = productDao.findById(productId);
        if (product == null) {
            logger.warn("更新商品状态失败: 商品不存在, productId={}", productId);
            return false;
        }
        
        // 检查状态转换是否合法
        Integer currentStatus = product.getStatus();
        
        // 草稿状态可以转为待审核或继续保持草稿状态
        if (currentStatus.equals(Product.STATUS_DRAFT) && 
            (status.equals(Product.STATUS_PENDING_REVIEW) || status.equals(Product.STATUS_DRAFT))) {
            return productDao.updateProductStatus(productId, status, sellerId);
        }
        
        // 在售状态可以转为下架状态
        if (currentStatus.equals(Product.STATUS_ONSALE) && status.equals(Product.STATUS_DELISTED)) {
            return productDao.updateProductStatus(productId, status, sellerId);
        }
        
        // 下架状态可以重新上架
        if (currentStatus.equals(Product.STATUS_DELISTED) && status.equals(Product.STATUS_ONSALE)) {
            return productDao.updateProductStatus(productId, status, sellerId);
        }
        
        // 其他状态转换不允许
        logger.warn("更新商品状态失败: 不允许从状态 {} 转换到状态 {}", currentStatus, status);
        return false;
    }
    
    @Override
    public boolean deleteProduct(Long productId, Long sellerId) {
        if (productId == null || sellerId == null) {
            logger.warn("删除商品失败: 参数为空");
            return false;
        }
        
        // 检查商品是否属于该卖家
        if (!isProductOwnedBySeller(productId, sellerId)) {
            logger.warn("删除商品失败: 商品不属于该卖家, productId={}, sellerId={}", productId, sellerId);
            return false;
        }
        
        return productDao.deleteProduct(productId, sellerId);
    }
    
    @Override
    public Map<String, Object> findProducts(String keyword, Integer categoryId,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         String sortBy, String sortDirection,
                                         int pageNum, int pageSize) {
        logMethodStart("findProducts", keyword, categoryId, pageNum, pageSize);

        try {
            // 参数校验
            validatePagination(pageNum, pageSize);

            // 修正参数范围
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize < 1 || pageSize > 100) {
                pageSize = 10; // 默认每页10条数据，最大100条
            }
        } catch (IllegalArgumentException e) {
            logBusinessWarning("查询商品列表失败: {}", e.getMessage());
            return createEmptyPageResult();
        }

        try {
            // 调用DAO层查询
            Map<String, Object> result = productDao.findProducts(keyword, categoryId, minPrice, maxPrice,
                                         sortBy, sortDirection, pageNum, pageSize);

            logMethodSuccess("findProducts", result);
            return result;
        } catch (Exception e) {
            logMethodError("findProducts", e, keyword, categoryId, pageNum, pageSize);
            return createEmptyPageResult();
        }
    }
    
    @Override
    public ProductDetailVO getProductDetailById(Long productId, Long currentUserId) {
        logMethodStart("getProductDetailById", productId, currentUserId);

        try {
            validateId(productId, "productId");
        } catch (IllegalArgumentException e) {
            logBusinessWarning("查询商品详情失败: {}", e.getMessage());
            return null;
        }
        
        // 查询商品详情
        ProductDetailVO productDetail = productDao.findProductDetailById(productId);
        
        // 商品不存在
        if (productDetail == null) {
            logBusinessWarning("查询商品详情失败: 商品不存在, productId={}", productId);
            return null;
        }

        // 检查权限：只有商品所有者和管理员才能查看非在售商品
        if (productDetail.getStatus() != Product.STATUS_ONSALE &&
            (currentUserId == null || !productDetail.getSellerId().equals(currentUserId))) {
            logBusinessWarning("查询商品详情失败: 无权查看非在售商品, productId={}, currentUserId={}, sellerId={}, status={}",
                     productId, currentUserId, productDetail.getSellerId(), productDetail.getStatus());
            return null;
        }

        logMethodSuccess("getProductDetailById", productDetail);
        return productDetail;
    }
    
    /**
     * 根据内容类型获取文件扩展名
     */
    private String getFileExtension(String contentType) {
        switch (contentType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/webp":
                return ".webp";
            default:
                return ".jpg";
        }
    }
} 