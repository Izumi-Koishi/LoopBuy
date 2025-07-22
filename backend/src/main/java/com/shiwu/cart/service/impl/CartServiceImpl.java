package com.shiwu.cart.service.impl;

import com.shiwu.cart.dao.CartDao;
import com.shiwu.cart.model.*;
import com.shiwu.cart.service.CartService;
import com.shiwu.framework.annotation.Autowired;
import com.shiwu.framework.annotation.Service;
import com.shiwu.framework.service.BaseService;
import com.shiwu.product.dao.ProductDao;
import com.shiwu.product.model.Product;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务实现类 - MVC框架版本
 */
@Service
public class CartServiceImpl extends BaseService implements CartService {

    @Autowired
    private CartDao cartDao;

    @Autowired
    private ProductDao productDao;

    public CartServiceImpl() {
        // 为了兼容测试，在无参构造函数中初始化DAO
        this.cartDao = new CartDao();
        this.productDao = new ProductDao();
        logger.info("CartServiceImpl初始化完成 - 使用MVC框架依赖注入");
    }

    // 兼容性构造函数，支持渐进式迁移
    public CartServiceImpl(CartDao cartDao, ProductDao productDao) {
        this.cartDao = cartDao;
        this.productDao = productDao;
        logger.info("CartServiceImpl初始化完成 - 使用兼容性构造函数");
    }
    
    @Override
    public CartOperationResult addToCart(CartAddDTO dto, Long userId) {
        // 参数验证
        if (dto == null || dto.getProductId() == null || userId == null) {
            logger.warn("添加商品到购物车失败: 参数不能为空");
            return CartOperationResult.failure(CartErrorCode.INVALID_PARAMS, CartErrorCode.MSG_INVALID_PARAMS);
        }

        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            logger.warn("添加商品到购物车失败: 商品数量无效, quantity={}", dto.getQuantity());
            return CartOperationResult.failure(CartErrorCode.INVALID_QUANTITY, CartErrorCode.MSG_INVALID_QUANTITY);
        }

        // 检查商品是否存在
        Product product = productDao.findById(dto.getProductId());
        if (product == null) {
            logger.warn("添加商品到购物车失败: 商品不存在, productId={}", dto.getProductId());
            return CartOperationResult.failure(CartErrorCode.PRODUCT_NOT_FOUND, CartErrorCode.MSG_PRODUCT_NOT_FOUND);
        }

        // 检查商品状态是否为在售
        if (product.getStatus() != Product.STATUS_ONSALE) {
            logger.warn("添加商品到购物车失败: 商品当前不可购买, productId={}, status={}",
                       dto.getProductId(), product.getStatus());
            return CartOperationResult.failure(CartErrorCode.PRODUCT_NOT_AVAILABLE, CartErrorCode.MSG_PRODUCT_NOT_AVAILABLE);
        }

        // 检查是否为自己的商品
        if (product.getSellerId().equals(userId)) {
            logger.warn("添加商品到购物车失败: 不能购买自己的商品, productId={}, sellerId={}, userId={}",
                       dto.getProductId(), product.getSellerId(), userId);
            return CartOperationResult.failure(CartErrorCode.CANT_BUY_OWN_PRODUCT, CartErrorCode.MSG_CANT_BUY_OWN_PRODUCT);
        }

        // 检查商品是否已在购物车中
        if (cartDao.existsInCart(userId, dto.getProductId())) {
            logger.warn("添加商品到购物车失败: 商品已在购物车中, userId={}, productId={}",
                       userId, dto.getProductId());
            return CartOperationResult.failure(CartErrorCode.PRODUCT_ALREADY_IN_CART, CartErrorCode.MSG_PRODUCT_ALREADY_IN_CART);
        }

        // 创建购物车项
        CartItem cartItem = new CartItem(userId, dto.getProductId(), dto.getQuantity());

        // 添加到购物车
        boolean success = cartDao.addToCart(cartItem);
        if (success) {
            // 返回购物车中的商品总数
            int totalItems = cartDao.getCartItemCount(userId);
            Map<String, Object> data = new HashMap<>();
            data.put("totalItems", totalItems);
            logger.info("添加商品到购物车成功: userId={}, productId={}, totalItems={}",
                       userId, dto.getProductId(), totalItems);
            return CartOperationResult.success(data);
        } else {
            logger.error("添加商品到购物车失败: 数据库操作失败, userId={}, productId={}",
                        userId, dto.getProductId());
            return CartOperationResult.failure(CartErrorCode.ADD_TO_CART_FAILED, CartErrorCode.MSG_ADD_TO_CART_FAILED);
        }
    }
    
    @Override
    public CartOperationResult getCart(Long userId) {
        if (userId == null) {
            logger.warn("获取购物车失败: 用户ID不能为空");
            return CartOperationResult.failure(CartErrorCode.INVALID_PARAMS, "用户ID不能为空");
        }

        try {
            // 获取购物车项列表
            List<CartItemVO> cartItems = cartDao.findCartItemsByUserId(userId);

            // 计算总价格（只计算可用商品）
            BigDecimal totalPrice = BigDecimal.ZERO;
            int availableItemCount = 0;
            int unavailableItemCount = 0;

            for (CartItemVO item : cartItems) {
                if (item.getAvailable() && item.getProduct() != null && item.getProduct().getPrice() != null) {
                    totalPrice = totalPrice.add(item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity())));
                    availableItemCount++;
                } else {
                    unavailableItemCount++;
                }
            }

            // 创建购物车视图对象
            CartVO cartVO = new CartVO();
            cartVO.setItems(cartItems);
            cartVO.setTotalItems(cartItems.size());
            cartVO.setTotalPrice(totalPrice);

            logger.info("获取购物车成功: userId={}, totalItems={}, availableItems={}, unavailableItems={}, totalPrice={}",
                       userId, cartItems.size(), availableItemCount, unavailableItemCount, totalPrice);

            return CartOperationResult.success(cartVO);
        } catch (Exception e) {
            logger.error("获取购物车失败: userId={}, error={}", userId, e.getMessage(), e);
            return CartOperationResult.failure(CartErrorCode.SYSTEM_ERROR, CartErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    @Override
    public CartOperationResult removeFromCart(Long productId, Long userId) {
        if (productId == null || userId == null) {
            logger.warn("从购物车移除商品失败: 参数不能为空");
            return CartOperationResult.failure(CartErrorCode.INVALID_PARAMS, CartErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            // 检查商品是否在购物车中
            if (!cartDao.existsInCart(userId, productId)) {
                logger.warn("从购物车移除商品失败: 商品不在购物车中, userId={}, productId={}", userId, productId);
                return CartOperationResult.failure(CartErrorCode.CART_ITEM_NOT_FOUND, CartErrorCode.MSG_CART_ITEM_NOT_FOUND);
            }

            boolean success = cartDao.removeFromCart(userId, productId);
            if (success) {
                logger.info("从购物车移除商品成功: userId={}, productId={}", userId, productId);
                // 返回更新后的购物车商品总数
                int totalItems = cartDao.getCartItemCount(userId);
                Map<String, Object> data = new HashMap<>();
                data.put("totalItems", totalItems);
                return CartOperationResult.success(data);
            } else {
                logger.error("从购物车移除商品失败: 数据库操作失败, userId={}, productId={}", userId, productId);
                return CartOperationResult.failure(CartErrorCode.REMOVE_FROM_CART_FAILED, CartErrorCode.MSG_REMOVE_FROM_CART_FAILED);
            }
        } catch (Exception e) {
            logger.error("从购物车移除商品失败: userId={}, productId={}, error={}", userId, productId, e.getMessage(), e);
            return CartOperationResult.failure(CartErrorCode.SYSTEM_ERROR, CartErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    @Override
    public CartOperationResult batchRemoveFromCart(List<Long> productIds, Long userId) {
        if (productIds == null || productIds.isEmpty() || userId == null) {
            logger.warn("批量从购物车移除商品失败: 参数不能为空");
            return CartOperationResult.failure(CartErrorCode.INVALID_PARAMS, CartErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            boolean success = cartDao.batchRemoveFromCart(userId, productIds);
            if (success) {
                logger.info("批量从购物车移除商品成功: userId={}, productIds={}", userId, productIds);
                // 返回更新后的购物车商品总数
                int totalItems = cartDao.getCartItemCount(userId);
                Map<String, Object> data = new HashMap<>();
                data.put("totalItems", totalItems);
                data.put("removedCount", productIds.size());
                return CartOperationResult.success(data);
            } else {
                logger.error("批量从购物车移除商品失败: 数据库操作失败, userId={}, productIds={}", userId, productIds);
                return CartOperationResult.failure(CartErrorCode.REMOVE_FROM_CART_FAILED, CartErrorCode.MSG_REMOVE_FROM_CART_FAILED);
            }
        } catch (Exception e) {
            logger.error("批量从购物车移除商品失败: userId={}, productIds={}, error={}", userId, productIds, e.getMessage(), e);
            return CartOperationResult.failure(CartErrorCode.SYSTEM_ERROR, CartErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    @Override
    public CartOperationResult clearCart(Long userId) {
        if (userId == null) {
            logger.warn("清空购物车失败: 用户ID不能为空");
            return CartOperationResult.failure(CartErrorCode.INVALID_PARAMS, CartErrorCode.MSG_INVALID_PARAMS);
        }

        try {
            boolean success = cartDao.clearCart(userId);
            if (success) {
                logger.info("清空购物车成功: userId={}", userId);
                Map<String, Object> data = new HashMap<>();
                data.put("totalItems", 0);
                return CartOperationResult.success(data);
            } else {
                logger.error("清空购物车失败: 数据库操作失败, userId={}", userId);
                return CartOperationResult.failure(CartErrorCode.SYSTEM_ERROR, CartErrorCode.MSG_SYSTEM_ERROR);
            }
        } catch (Exception e) {
            logger.error("清空购物车失败: userId={}, error={}", userId, e.getMessage(), e);
            return CartOperationResult.failure(CartErrorCode.SYSTEM_ERROR, CartErrorCode.MSG_SYSTEM_ERROR);
        }
    }
    
    @Override
    public int getCartItemCount(Long userId) {
        if (userId == null) {
            logger.warn("获取购物车商品总数失败: 用户ID不能为空");
            return 0;
        }
        
        return cartDao.getCartItemCount(userId);
    }
}
