import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../auth/contexts/AuthContext';
import AdminLayout from '../../../shared/components/AdminLayout';
import productManagementApi from '../services/productManagementApi';
import { formatCurrency, formatDate } from '../../../shared/utils/format';
import {
  Product,
  ProductQueryParams,
  ProductManagementState,
  ProductStatus,
  ProductStatusLabels,
  ProductStatusColors
} from '../types/product-management';
import '../../../shared/styles/admin-pages.css';

const ProductManagementPageFinal: React.FC = () => {
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

  // 筛选状态
  const [filters, setFilters] = useState({
    keyword: '',
    status: '',
    category: '',
    startDate: '',
    endDate: ''
  });

  // 获取商品列表
  const fetchProducts = useCallback(async (params?: Partial<ProductQueryParams>) => {
    setState(prev => ({ ...prev, loading: true }));
    
    try {
      const queryParams = { ...state.filters, ...params };
      const response = await productManagementApi.getProducts(queryParams);
      
      if (response.success && response.data) {
        // 适配后端返回的数据结构
        const backendData = response.data as any;
        setState(prev => ({
          ...prev,
          products: backendData.products || [],
          pagination: {
            current: backendData.page || 1,
            pageSize: backendData.pageSize || 20,
            total: backendData.totalCount || 0
          },
          loading: false
        }));
        console.log('成功获取商品列表:', backendData);
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

  // 刷新数据
  const handleRefresh = () => {
    fetchProducts();
  };

  // 搜索处理
  const handleSearch = () => {
    const searchParams = {
      keyword: filters.keyword,
      status: filters.status ? parseInt(filters.status) : undefined,
      category: filters.category || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      pageNum: 1
    };
    fetchProducts(searchParams);
  };

  // 重置筛选
  const handleResetFilters = () => {
    setFilters({
      keyword: '',
      status: '',
      category: '',
      startDate: '',
      endDate: ''
    });
    fetchProducts({ pageNum: 1 });
  };

  // 审核通过商品
  const handleApproveProduct = async (productId: number, productTitle: string) => {
    if (!confirm(`确定要审核通过商品"${productTitle}"吗？`)) return;

    try {
      const response = await productManagementApi.approveProduct(productId, { reason: '审核通过' });
      if (response.success) {
        alert('商品审核通过成功');
        fetchProducts(); // 刷新列表
      } else {
        alert(`审核失败: ${response.error?.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('审核商品失败:', error);
      alert('操作失败，请稍后重试');
    }
  };

  // 审核拒绝商品
  const handleRejectProduct = async (productId: number, productTitle: string) => {
    const reason = prompt(`请输入拒绝商品"${productTitle}"的原因:`);
    if (!reason || reason.trim() === '') {
      alert('请填写拒绝原因');
      return;
    }

    try {
      const response = await productManagementApi.rejectProduct(productId, { reason });
      if (response.success) {
        alert('商品审核拒绝成功');
        fetchProducts(); // 刷新列表
      } else {
        alert(`审核失败: ${response.error?.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('审核商品失败:', error);
      alert('操作失败，请稍后重试');
    }
  };

  // 下架商品
  const handleDelistProduct = async (productId: number, productTitle: string) => {
    const reason = prompt(`请输入下架商品"${productTitle}"的原因:`);
    if (!reason || reason.trim() === '') {
      alert('请填写下架原因');
      return;
    }

    try {
      const response = await productManagementApi.delistProduct(productId, { reason });
      if (response.success) {
        alert('商品下架成功');
        fetchProducts();
      } else {
        alert(`下架失败: ${response.error?.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('下架商品失败:', error);
      alert('操作失败，请稍后重试');
    }
  };

  // 删除商品
  const handleDeleteProduct = async (productId: number, productTitle: string) => {
    if (!confirm(`确定要删除商品"${productTitle}"吗？此操作不可恢复。`)) return;

    try {
      const response = await productManagementApi.deleteProduct(productId);
      if (response.success) {
        alert('商品删除成功');
        fetchProducts();
      } else {
        alert(`删除失败: ${response.error?.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('删除商品失败:', error);
      alert('操作失败，请稍后重试');
    }
  };

  // 分页处理
  const handlePageChange = (page: number) => {
    fetchProducts({ pageNum: page });
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
          {/* 商品统计 */}
          <div className="stats-container">
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#1890ff', marginBottom: '8px' }}>
                {state.pagination.total}
              </div>
              <div style={{ color: '#6b7280' }}>总商品数</div>
            </div>
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#faad14', marginBottom: '8px' }}>
                {state.products.filter(p => p.status === ProductStatus.PENDING_REVIEW).length}
              </div>
              <div style={{ color: '#6b7280' }}>待审核</div>
            </div>
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#52c41a', marginBottom: '8px' }}>
                {state.products.filter(p => p.status === ProductStatus.ONSALE).length}
              </div>
              <div style={{ color: '#6b7280' }}>在售中</div>
            </div>
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#f5222d', marginBottom: '8px' }}>
                {state.products.filter(p => p.status === ProductStatus.OFFSALE).length}
              </div>
              <div style={{ color: '#6b7280' }}>已下架</div>
            </div>
          </div>

          {/* 筛选器 */}
          <div className="filters-container">
            <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', fontWeight: '600' }}>筛选条件</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '16px' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '4px', fontSize: '14px', fontWeight: '500' }}>关键词</label>
                <input
                  type="text"
                  value={filters.keyword}
                  onChange={(e) => setFilters(prev => ({ ...prev, keyword: e.target.value }))}
                  placeholder="商品名称或描述"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    border: '1px solid #d1d5db',
                    borderRadius: '6px',
                    fontSize: '14px'
                  }}
                />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '4px', fontSize: '14px', fontWeight: '500' }}>状态</label>
                <select
                  value={filters.status}
                  onChange={(e) => setFilters(prev => ({ ...prev, status: e.target.value }))}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    border: '1px solid #d1d5db',
                    borderRadius: '6px',
                    fontSize: '14px'
                  }}
                >
                  <option value="">全部状态</option>
                  <option value="0">待审核</option>
                  <option value="1">在售中</option>
                  <option value="2">已售出</option>
                  <option value="3">已下架</option>
                </select>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                onClick={handleSearch}
                style={{
                  padding: '8px 16px',
                  background: '#3b82f6',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  fontSize: '14px',
                  cursor: 'pointer'
                }}
              >
                搜索
              </button>
              <button
                onClick={handleResetFilters}
                style={{
                  padding: '8px 16px',
                  background: '#6b7280',
                  color: 'white',
                  border: 'none',
                  borderRadius: '6px',
                  fontSize: '14px',
                  cursor: 'pointer'
                }}
              >
                重置
              </button>
            </div>
          </div>

          {/* 商品列表 */}
          <div className="list-container">
            <div style={{ padding: '20px', borderBottom: '1px solid #e5e7eb' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>
                商品列表 ({state.products.length} 个商品)
              </h3>
            </div>

            {state.loading ? (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <div style={{
                  width: '40px',
                  height: '40px',
                  border: '4px solid #f3f4f6',
                  borderTop: '4px solid #3b82f6',
                  borderRadius: '50%',
                  animation: 'spin 1s linear infinite',
                  margin: '0 auto 16px'
                }}></div>
                <p>加载商品数据中...</p>
              </div>
            ) : state.products.length === 0 ? (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <p>暂无商品数据</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ background: '#f9fafb' }}>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        商品信息
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        价格
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        状态
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        卖家
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        创建时间
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        操作
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {state.products.map(product => (
                      <tr key={product.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                        <td style={{ padding: '12px' }}>
                          <div>
                            <div style={{ fontWeight: '500', marginBottom: '4px' }}>
                              {product.title}
                            </div>
                            {product.description && (
                              <div style={{ fontSize: '14px', color: '#6b7280' }}>
                                {product.description.length > 50
                                  ? `${product.description.substring(0, 50)}...`
                                  : product.description
                                }
                              </div>
                            )}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <span style={{ fontWeight: '600', color: '#dc2626' }}>
                            {formatCurrency(product.price)}
                          </span>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <span style={{
                            padding: '4px 8px',
                            borderRadius: '4px',
                            fontSize: '12px',
                            fontWeight: '500',
                            background: `${ProductStatusColors[product.status]}20`,
                            color: ProductStatusColors[product.status]
                          }}>
                            {ProductStatusLabels[product.status]}
                          </span>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ fontSize: '14px' }}>
                            {product.sellerName}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ fontSize: '14px', color: '#6b7280' }}>
                            {formatDate(product.createTime, 'YYYY-MM-DD HH:mm')}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                            {product.status === ProductStatus.PENDING_REVIEW && (
                              <>
                                <button
                                  onClick={() => handleApproveProduct(product.id, product.title)}
                                  style={{
                                    padding: '4px 8px',
                                    fontSize: '12px',
                                    border: '1px solid #10b981',
                                    background: '#10b981',
                                    color: 'white',
                                    borderRadius: '4px',
                                    cursor: 'pointer'
                                  }}
                                >
                                  通过
                                </button>
                                <button
                                  onClick={() => handleRejectProduct(product.id, product.title)}
                                  style={{
                                    padding: '4px 8px',
                                    fontSize: '12px',
                                    border: '1px solid #dc2626',
                                    background: '#dc2626',
                                    color: 'white',
                                    borderRadius: '4px',
                                    cursor: 'pointer'
                                  }}
                                >
                                  拒绝
                                </button>
                              </>
                            )}
                            {product.status === ProductStatus.ONSALE && (
                              <button
                                onClick={() => handleDelistProduct(product.id, product.title)}
                                style={{
                                  padding: '4px 8px',
                                  fontSize: '12px',
                                  border: '1px solid #f59e0b',
                                  background: '#f59e0b',
                                  color: 'white',
                                  borderRadius: '4px',
                                  cursor: 'pointer'
                                }}
                              >
                                下架
                              </button>
                            )}
                            <button
                              onClick={() => handleDeleteProduct(product.id, product.title)}
                              style={{
                                padding: '4px 8px',
                                fontSize: '12px',
                                border: '1px solid #dc2626',
                                background: 'white',
                                color: '#dc2626',
                                borderRadius: '4px',
                                cursor: 'pointer'
                              }}
                            >
                              删除
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* 分页 */}
            {state.pagination.total > state.pagination.pageSize && (
              <div style={{ padding: '20px', borderTop: '1px solid #e5e7eb', textAlign: 'center' }}>
                <div style={{ display: 'inline-flex', gap: '8px', alignItems: 'center' }}>
                  <button
                    onClick={() => handlePageChange(state.pagination.current - 1)}
                    disabled={state.pagination.current <= 1}
                    style={{
                      padding: '8px 12px',
                      border: '1px solid #d1d5db',
                      background: 'white',
                      borderRadius: '4px',
                      cursor: state.pagination.current <= 1 ? 'not-allowed' : 'pointer',
                      opacity: state.pagination.current <= 1 ? 0.5 : 1
                    }}
                  >
                    上一页
                  </button>
                  <span style={{ padding: '0 16px', fontSize: '14px', color: '#6b7280' }}>
                    第 {state.pagination.current} 页，共 {Math.ceil(state.pagination.total / state.pagination.pageSize)} 页
                  </span>
                  <button
                    onClick={() => handlePageChange(state.pagination.current + 1)}
                    disabled={state.pagination.current >= Math.ceil(state.pagination.total / state.pagination.pageSize)}
                    style={{
                      padding: '8px 12px',
                      border: '1px solid #d1d5db',
                      background: 'white',
                      borderRadius: '4px',
                      cursor: state.pagination.current >= Math.ceil(state.pagination.total / state.pagination.pageSize) ? 'not-allowed' : 'pointer',
                      opacity: state.pagination.current >= Math.ceil(state.pagination.total / state.pagination.pageSize) ? 0.5 : 1
                    }}
                  >
                    下一页
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </AdminLayout>
  );
};

export default ProductManagementPageFinal;
