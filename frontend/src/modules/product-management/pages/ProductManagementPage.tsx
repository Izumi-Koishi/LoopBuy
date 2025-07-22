import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../auth/contexts/AuthContext';
import AdminLayout from '../../../shared/components/AdminLayout';
import ProductFilters from '../components/ProductFilters';
import ProductList from '../components/ProductList';
import ProductAuditModal from '../components/ProductAuditModal';
import productManagementApi from '../services/productManagementApi';
import {
  Product,
  ProductQueryParams,
  ProductAction,
  ProductActionConfigs,
  BatchOperationParams,
  ProductManagementState
} from '../types/product-management';
import './ProductManagementPage.css';
import '../../../shared/styles/admin-pages.css';

const ProductManagementPage: React.FC = () => {
  const { admin } = useAuth();
  
  // 页面状态
  const [state, setState] = useState<ProductManagementState>({
    products: [],
    loading: false,
    selectedProductIds: [],
    filters: {
      pageNum: 1,
      pageSize: 20,
      sortBy: 'create_time',
      sortDirection: 'DESC'
    },
    pagination: {
      current: 1,
      pageSize: 20,
      total: 0
    },
    auditModalVisible: false
  });

  // 获取商品列表
  const fetchProducts = useCallback(async (params?: Partial<ProductQueryParams>) => {
    setState(prev => ({ ...prev, loading: true }));
    
    try {
      const queryParams = { ...state.filters, ...params };
      const response = await productManagementApi.getProducts(queryParams);
      
      if (response.success && response.data) {
        setState(prev => ({
          ...prev,
          products: response.data!.products,
          pagination: {
            current: response.data!.pagination.pageNum,
            pageSize: response.data!.pagination.pageSize,
            total: response.data!.pagination.total
          },
          loading: false
        }));
      } else {
        console.error('获取商品列表失败:', response.error);
        setState(prev => ({ ...prev, loading: false }));
      }
    } catch (error) {
      console.error('获取商品列表异常:', error);
      setState(prev => ({ ...prev, loading: false }));
    }
  }, [state.filters]);

  // 初始加载
  useEffect(() => {
    fetchProducts();
  }, []);

  // 处理筛选条件变化
  const handleFiltersChange = (newFilters: ProductQueryParams) => {
    setState(prev => ({
      ...prev,
      filters: { ...newFilters, pageNum: 1 },
      selectedProductIds: []
    }));
    fetchProducts({ ...newFilters, pageNum: 1 });
  };

  // 重置筛选条件
  const handleFiltersReset = () => {
    const resetFilters: ProductQueryParams = {
      pageNum: 1,
      pageSize: 20,
      sortBy: 'create_time',
      sortDirection: 'DESC'
    };
    setState(prev => ({
      ...prev,
      filters: resetFilters,
      selectedProductIds: []
    }));
    fetchProducts(resetFilters);
  };

  // 处理选择变化
  const handleSelectionChange = (selectedIds: number[]) => {
    setState(prev => ({ ...prev, selectedProductIds: selectedIds }));
  };

  // 处理排序
  const handleSort = (sortBy: string, sortDirection: 'ASC' | 'DESC') => {
    const newFilters = { ...state.filters, sortBy, sortDirection };
    setState(prev => ({ ...prev, filters: newFilters }));
    fetchProducts(newFilters);
  };

  // 处理分页
  const handlePageChange = (page: number, pageSize: number) => {
    const newFilters = { ...state.filters, pageNum: page, pageSize };
    setState(prev => ({ ...prev, filters: newFilters }));
    fetchProducts(newFilters);
  };

  // 处理商品操作
  const handleProductAction = (action: ProductAction, productIds: number[]) => {
    setState(prev => ({
      ...prev,
      currentAction: action,
      selectedProductIds: productIds,
      auditModalVisible: true
    }));
  };

  // 处理批量操作
  const handleBatchAction = (action: ProductAction) => {
    if (state.selectedProductIds.length === 0) {
      alert('请先选择要操作的商品');
      return;
    }
    handleProductAction(action, state.selectedProductIds);
  };

  // 确认操作
  const handleConfirmAction = async (params: BatchOperationParams) => {
    if (!state.currentAction) return;

    setState(prev => ({ ...prev, loading: true }));

    try {
      const response = await productManagementApi.batchOperation(state.currentAction, params);
      
      if (response.success) {
        // 操作成功，刷新列表
        await fetchProducts();
        setState(prev => ({
          ...prev,
          auditModalVisible: false,
          selectedProductIds: [],
          currentAction: undefined,
          loading: false
        }));
        
        // 显示成功消息
        alert(response.message || '操作成功');
      } else {
        setState(prev => ({ ...prev, loading: false }));
        alert(response.error?.message || '操作失败');
      }
    } catch (error) {
      setState(prev => ({ ...prev, loading: false }));
      alert('操作异常，请稍后重试');
    }
  };

  // 取消操作
  const handleCancelAction = () => {
    setState(prev => ({
      ...prev,
      auditModalVisible: false,
      currentAction: undefined
    }));
  };

  // 刷新数据
  const handleRefresh = () => {
    fetchProducts();
  };

  // 获取选中商品的标题
  const getSelectedProductTitles = (): string[] => {
    return state.selectedProductIds.map(id => {
      const product = state.products.find(p => p.id === id);
      return product?.title || '';
    });
  };

  return (
    <AdminLayout>
      <div className="product-management-page">
        {/* 页面头部 */}
        <div className="page-header">
          <div className="page-title-section">
            <h1 className="page-title">商品审核与管理</h1>
            <p className="page-subtitle">管理平台商品，进行审核、下架、删除等操作</p>
          </div>
          <div className="page-actions">
            <button onClick={handleRefresh} className="refresh-btn" disabled={state.loading}>
              {state.loading ? '刷新中...' : '刷新数据'}
            </button>
          </div>
        </div>

      {/* 页面内容 */}
      <div className="page-content">
        {/* 筛选器 */}
        <ProductFilters
          filters={state.filters}
          onFiltersChange={handleFiltersChange}
          onReset={handleFiltersReset}
        />

        {/* 批量操作栏 */}
        {state.selectedProductIds.length > 0 && (
          <div className="batch-actions">
            <div className="batch-info">
              已选择 {state.selectedProductIds.length} 个商品
            </div>
            <div className="batch-buttons">
              <button
                onClick={() => handleBatchAction(ProductAction.APPROVE)}
                className="batch-btn approve-btn"
              >
                <span className="btn-icon">✓</span>
                批量审核通过
              </button>
              <button
                onClick={() => handleBatchAction(ProductAction.REJECT)}
                className="batch-btn reject-btn"
              >
                <span className="btn-icon">✗</span>
                批量审核拒绝
              </button>
              <button
                onClick={() => handleBatchAction(ProductAction.DELIST)}
                className="batch-btn delist-btn"
              >
                <span className="btn-icon">↓</span>
                批量下架
              </button>
              <button
                onClick={() => handleBatchAction(ProductAction.DELETE)}
                className="batch-btn delete-btn"
              >
                <span className="btn-icon">🗑</span>
                批量删除
              </button>
            </div>
          </div>
        )}

        {/* 商品列表 */}
        <ProductList
          products={state.products}
          loading={state.loading}
          selectedProductIds={state.selectedProductIds}
          onSelectionChange={handleSelectionChange}
          onProductAction={handleProductAction}
          onSort={handleSort}
          pagination={state.pagination}
          onPageChange={handlePageChange}
        />
      </div>

      {/* 审核模态框 */}
      {state.currentAction && (
        <ProductAuditModal
          visible={state.auditModalVisible}
          action={state.currentAction}
          productIds={state.selectedProductIds}
          productTitles={getSelectedProductTitles()}
          onConfirm={handleConfirmAction}
          onCancel={handleCancelAction}
          loading={state.loading}
        />
      )}
      </div>
    </AdminLayout>
  );
};

export default ProductManagementPage;
