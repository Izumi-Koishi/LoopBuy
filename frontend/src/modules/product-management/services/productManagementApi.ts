import api from '../../../shared/services/baseApi';
import {
  ProductQueryParams,
  ProductQueryResponse,
  ProductDetailResponse,
  ProductManageParams,
  ProductManageResponse,
  BatchOperationParams,
  ProductAction
} from '../types/product-management';

// 产品管理API服务类
class ProductManagementApiService {

  // 获取商品列表
  async getProducts(params: ProductQueryParams = {}): Promise<ProductQueryResponse> {
    try {
      const queryParams = new URLSearchParams();
      
      if (params.keyword) queryParams.append('keyword', params.keyword);
      if (params.status !== undefined) queryParams.append('status', params.status.toString());
      if (params.sellerId) queryParams.append('sellerId', params.sellerId.toString());
      if (params.categoryId) queryParams.append('categoryId', params.categoryId.toString());
      if (params.pageNum) queryParams.append('pageNum', params.pageNum.toString());
      if (params.pageSize) queryParams.append('pageSize', params.pageSize.toString());
      if (params.sortBy) queryParams.append('sortBy', params.sortBy);
      if (params.sortDirection) queryParams.append('sortDirection', params.sortDirection);

      const response = await api.get(`/admin/products?${queryParams.toString()}`);
      return response.data;
    } catch (error: any) {
      console.error('获取商品列表失败:', error);
      return {
        success: false,
        error: {
          code: 'FETCH_PRODUCTS_ERROR',
          message: error.response?.data?.message || '获取商品列表失败'
        }
      };
    }
  }

  // 获取商品详情
  async getProductDetail(productId: number): Promise<ProductDetailResponse> {
    try {
      const response = await api.get(`/admin/products/${productId}`);
      return response.data;
    } catch (error: any) {
      console.error('获取商品详情失败:', error);
      return {
        success: false,
        error: {
          code: 'FETCH_PRODUCT_DETAIL_ERROR',
          message: error.response?.data?.message || '获取商品详情失败'
        }
      };
    }
  }

  // 审核商品 - 与后端API对齐
  async approveProduct(productId: number, params: ProductManageParams = {}): Promise<ProductManageResponse> {
    try {
      const requestData = {
        action: 'APPROVE',
        reason: params.reason || ''
      };
      const response = await api.post(`/admin/products/${productId}/approve`, requestData);
      return response.data;
    } catch (error: any) {
      console.error('审核通过商品失败:', error);
      if (error.response?.data) {
        return error.response.data;
      }
      return {
        success: false,
        error: {
          code: 'APPROVE_PRODUCT_ERROR',
          message: '审核通过商品失败',
          userTip: '请稍后重试或联系管理员'
        }
      };
    }
  }

  // 审核拒绝商品 - 与后端API对齐
  async rejectProduct(productId: number, params: ProductManageParams): Promise<ProductManageResponse> {
    try {
      if (!params.reason || params.reason.trim() === '') {
        return {
          success: false,
          error: {
            code: 'INVALID_REASON',
            message: '拒绝原因不能为空',
            userTip: '请填写拒绝原因'
          }
        };
      }

      const requestData = {
        action: 'REJECT',
        reason: params.reason
      };
      const response = await api.post(`/admin/products/${productId}/approve`, requestData);
      return response.data;
    } catch (error: any) {
      console.error('审核拒绝商品失败:', error);
      if (error.response?.data) {
        return error.response.data;
      }
      return {
        success: false,
        error: {
          code: 'REJECT_PRODUCT_ERROR',
          message: '审核拒绝商品失败',
          userTip: '请稍后重试或联系管理员'
        }
      };
    }
  }

  // 下架商品 - 与后端API对齐
  async delistProduct(productId: number, params: ProductManageParams = {}): Promise<ProductManageResponse> {
    try {
      const requestData = {
        reason: params.reason || '管理员下架'
      };
      const response = await api.post(`/admin/products/${productId}/remove`, requestData);
      return response.data;
    } catch (error: any) {
      console.error('下架商品失败:', error);
      if (error.response?.data) {
        return error.response.data;
      }
      return {
        success: false,
        error: {
          code: 'DELIST_PRODUCT_ERROR',
          message: '下架商品失败',
          userTip: '请稍后重试或联系管理员'
        }
      };
    }
  }

  // 删除商品 - 与后端API对齐
  async deleteProduct(productId: number): Promise<ProductManageResponse> {
    try {
      const response = await api.delete(`/admin/products/${productId}`);
      return response.data;
    } catch (error: any) {
      console.error('删除商品失败:', error);
      if (error.response?.data) {
        return error.response.data;
      }
      return {
        success: false,
        error: {
          code: 'DELETE_PRODUCT_ERROR',
          message: '删除商品失败',
          userTip: '请稍后重试或联系管理员'
        }
      };
    }
  }

  // 批量操作商品
  async batchOperation(action: ProductAction, params: BatchOperationParams): Promise<ProductManageResponse> {
    try {
      const promises = params.productIds.map(productId => {
        switch (action) {
          case ProductAction.APPROVE:
            return this.approveProduct(productId, { reason: params.reason });
          case ProductAction.REJECT:
            return this.rejectProduct(productId, { reason: params.reason || '' });
          case ProductAction.DELIST:
            return this.delistProduct(productId, { reason: params.reason });
          case ProductAction.DELETE:
            return this.deleteProduct(productId);
          default:
            throw new Error(`不支持的操作类型: ${action}`);
        }
      });

      const results = await Promise.allSettled(promises);
      const successCount = results.filter(result => 
        result.status === 'fulfilled' && result.value.success
      ).length;
      const failureCount = results.length - successCount;

      if (failureCount === 0) {
        return {
          success: true,
          message: `批量操作成功，共处理 ${successCount} 个商品`
        };
      } else if (successCount === 0) {
        return {
          success: false,
          error: {
            code: 'BATCH_OPERATION_FAILED',
            message: `批量操作失败，共 ${failureCount} 个商品操作失败`
          }
        };
      } else {
        return {
          success: true,
          message: `批量操作部分成功，成功 ${successCount} 个，失败 ${failureCount} 个`
        };
      }
    } catch (error: any) {
      console.error('批量操作失败:', error);
      return {
        success: false,
        error: {
          code: 'BATCH_OPERATION_ERROR',
          message: error.message || '批量操作失败'
        }
      };
    }
  }

  // 获取商品分类列表（用于筛选）
  async getCategories(): Promise<Array<{ label: string; value: number }>> {
    try {
      const response = await api.get('/categories/');
      if (response.data.success && response.data.data) {
        const categories = response.data.data.map((category: any) => ({
          label: category.name,
          value: category.id
        }));
        // 添加"全部分类"选项
        return [{ label: '全部分类', value: 0 }, ...categories];
      } else {
        throw new Error('获取分类数据失败');
      }
    } catch (error) {
      console.error('获取分类列表失败:', error);
      // 返回默认分类选项
      return [
        { label: '全部分类', value: 0 },
        { label: '电子产品', value: 1 },
        { label: '图书教材', value: 2 },
        { label: '生活用品', value: 3 },
        { label: '服装配饰', value: 4 },
        { label: '运动器材', value: 5 }
      ];
    }
  }
}

// 导出单例实例
export const productManagementApi = new ProductManagementApiService();
export default productManagementApi;
