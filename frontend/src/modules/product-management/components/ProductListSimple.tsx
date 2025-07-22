import React from 'react';
import {
  Product,
  ProductStatus,
  ProductStatusLabels,
  ProductStatusColors
} from '../types/product-management';
import { formatCurrency, formatDate } from '../../../shared/utils/format';

interface ProductListSimpleProps {
  products: Product[];
  loading: boolean;
}

const ProductListSimple: React.FC<ProductListSimpleProps> = ({
  products,
  loading
}) => {
  if (loading) {
    return (
      <div style={{ 
        background: 'white', 
        padding: '40px', 
        borderRadius: '8px',
        textAlign: 'center',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
      }}>
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
    );
  }

  if (products.length === 0) {
    return (
      <div style={{ 
        background: 'white', 
        padding: '40px', 
        borderRadius: '8px',
        textAlign: 'center',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
      }}>
        <p>暂无商品数据</p>
      </div>
    );
  }

  return (
    <div style={{ 
      background: 'white', 
      borderRadius: '8px',
      boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
      overflow: 'hidden'
    }}>
      <div style={{ padding: '20px', borderBottom: '1px solid #e5e7eb' }}>
        <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>
          商品列表 ({products.length} 个商品)
        </h3>
      </div>
      
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
            {products.map(product => (
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
                  <div style={{ display: 'flex', gap: '8px' }}>
                    {product.status === ProductStatus.PENDING_REVIEW && (
                      <>
                        <button style={{
                          padding: '4px 8px',
                          fontSize: '12px',
                          border: '1px solid #10b981',
                          background: '#10b981',
                          color: 'white',
                          borderRadius: '4px',
                          cursor: 'pointer'
                        }}>
                          通过
                        </button>
                        <button style={{
                          padding: '4px 8px',
                          fontSize: '12px',
                          border: '1px solid #dc2626',
                          background: '#dc2626',
                          color: 'white',
                          borderRadius: '4px',
                          cursor: 'pointer'
                        }}>
                          拒绝
                        </button>
                      </>
                    )}
                    {product.status === ProductStatus.ONSALE && (
                      <button style={{
                        padding: '4px 8px',
                        fontSize: '12px',
                        border: '1px solid #f59e0b',
                        background: '#f59e0b',
                        color: 'white',
                        borderRadius: '4px',
                        cursor: 'pointer'
                      }}>
                        下架
                      </button>
                    )}
                    <button style={{
                      padding: '4px 8px',
                      fontSize: '12px',
                      border: '1px solid #6b7280',
                      background: 'white',
                      color: '#6b7280',
                      borderRadius: '4px',
                      cursor: 'pointer'
                    }}>
                      详情
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ProductListSimple;
