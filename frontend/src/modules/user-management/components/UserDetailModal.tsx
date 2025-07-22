import React, { useEffect, useState } from 'react';
import { User, UserStatus } from '../types/user-management';
import { userManagementApi } from '../services/userManagementApi';

interface UserDetailModalProps {
  visible: boolean;
  userId: number | null;
  onClose: () => void;
}

const UserDetailModal: React.FC<UserDetailModalProps> = ({
  visible,
  userId,
  onClose
}) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 获取用户详情
  const fetchUserDetail = async (id: number) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await userManagementApi.getUserDetail(id);
      if (response.success && response.data) {
        setUser(response.data);
      } else {
        setError(response.error?.message || '获取用户详情失败');
      }
    } catch (err: any) {
      setError('获取用户详情异常');
      console.error('获取用户详情失败:', err);
    } finally {
      setLoading(false);
    }
  };

  // 当弹窗打开且有用户ID时获取详情
  useEffect(() => {
    if (visible && userId) {
      fetchUserDetail(userId);
    } else {
      setUser(null);
      setError(null);
    }
  }, [visible, userId]);

  // 获取状态标签
  const getStatusLabel = (status: UserStatus) => {
    switch (status) {
      case UserStatus.NORMAL:
        return { label: '正常', color: '#52c41a' };
      case UserStatus.BANNED:
        return { label: '封禁', color: '#f5222d' };
      case UserStatus.MUTED:
        return { label: '禁言', color: '#faad14' };
      default:
        return { label: '未知', color: '#d9d9d9' };
    }
  };

  // 格式化时间
  const formatDateTime = (dateTime: string | number[] | null) => {
    if (!dateTime) return '无';
    
    if (Array.isArray(dateTime)) {
      // 处理后端返回的数组格式 [2025, 7, 22, 10, 30, 0]
      const [year, month, day, hour = 0, minute = 0, second = 0] = dateTime;
      return `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:${second.toString().padStart(2, '0')}`;
    }
    
    if (typeof dateTime === 'string') {
      return new Date(dateTime).toLocaleString('zh-CN');
    }
    
    return '无';
  };

  if (!visible) return null;

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      backgroundColor: 'rgba(0, 0, 0, 0.5)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000
    }}>
      <div style={{
        backgroundColor: 'white',
        borderRadius: '8px',
        padding: '24px',
        width: '600px',
        maxWidth: '90vw',
        maxHeight: '80vh',
        overflow: 'auto',
        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)'
      }}>
        {/* 标题栏 */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '20px',
          borderBottom: '1px solid #f0f0f0',
          paddingBottom: '16px'
        }}>
          <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold' }}>
            用户详情
          </h2>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '20px',
              cursor: 'pointer',
              color: '#999',
              padding: '4px'
            }}
          >
            ×
          </button>
        </div>

        {/* 内容区域 */}
        <div>
          {loading && (
            <div style={{ textAlign: 'center', padding: '40px' }}>
              <div>加载中...</div>
            </div>
          )}

          {error && (
            <div style={{
              textAlign: 'center',
              padding: '40px',
              color: '#f5222d'
            }}>
              <div>❌ {error}</div>
              <button
                onClick={() => userId && fetchUserDetail(userId)}
                style={{
                  marginTop: '16px',
                  padding: '8px 16px',
                  backgroundColor: '#1890ff',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer'
                }}
              >
                重试
              </button>
            </div>
          )}

          {user && !loading && !error && (
            <div style={{ lineHeight: '1.6' }}>
              {/* 基本信息 */}
              <div style={{ marginBottom: '24px' }}>
                <h3 style={{ 
                  margin: '0 0 16px 0', 
                  fontSize: '16px', 
                  color: '#262626',
                  borderLeft: '3px solid #1890ff',
                  paddingLeft: '12px'
                }}>
                  基本信息
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  <div>
                    <strong>用户ID：</strong>{user.id}
                  </div>
                  <div>
                    <strong>用户名：</strong>{user.username}
                  </div>
                  <div>
                    <strong>邮箱：</strong>{user.email || '未设置'}
                  </div>
                  <div>
                    <strong>状态：</strong>
                    <span style={{
                      color: getStatusLabel(user.status).color,
                      fontWeight: 'bold'
                    }}>
                      {getStatusLabel(user.status).label}
                    </span>
                  </div>
                </div>
              </div>

              {/* 时间信息 */}
              <div style={{ marginBottom: '24px' }}>
                <h3 style={{ 
                  margin: '0 0 16px 0', 
                  fontSize: '16px', 
                  color: '#262626',
                  borderLeft: '3px solid #52c41a',
                  paddingLeft: '12px'
                }}>
                  时间信息
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                  <div>
                    <strong>注册时间：</strong>{formatDateTime(user.createTime)}
                  </div>
                  <div>
                    <strong>最后登录：</strong>{formatDateTime(user.lastLoginTime)}
                  </div>
                  <div>
                    <strong>更新时间：</strong>{formatDateTime(user.updateTime)}
                  </div>
                </div>
              </div>

              {/* 操作按钮 */}
              <div style={{
                display: 'flex',
                justifyContent: 'flex-end',
                gap: '12px',
                marginTop: '24px',
                paddingTop: '16px',
                borderTop: '1px solid #f0f0f0'
              }}>
                <button
                  onClick={onClose}
                  style={{
                    padding: '8px 16px',
                    backgroundColor: '#f5f5f5',
                    color: '#666',
                    border: '1px solid #d9d9d9',
                    borderRadius: '4px',
                    cursor: 'pointer'
                  }}
                >
                  关闭
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserDetailModal;
