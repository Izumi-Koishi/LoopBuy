import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../auth/contexts/AuthContext';
import AdminLayout from '../../../shared/components/AdminLayout';
import userManagementApi from '../services/userManagementApi';
import { formatDate } from '../../../shared/utils/format';
import {
  User,
  UserQueryParams,
  UserManagementState,
  UserStats,
  UserStatus,
  UserStatusLabels,
  UserStatusColors
} from '../types/user-management';
import '../../../shared/styles/admin-pages.css';

const UserManagementPageFinal: React.FC = () => {
  const { admin } = useAuth();
  
  // 页面状态
  const [state, setState] = useState<UserManagementState>({
    users: [],
    loading: false,
    selectedUserIds: [],
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
    actionModalVisible: false
  });

  // 用户统计信息
  const [userStats, setUserStats] = useState<UserStats | null>(null);

  // 筛选状态
  const [filters, setFilters] = useState({
    keyword: '',
    status: '',
    startDate: '',
    endDate: ''
  });

  // 获取用户列表
  const fetchUsers = useCallback(async (params?: Partial<UserQueryParams>) => {
    setState(prev => ({ ...prev, loading: true }));

    try {
      // 使用默认参数，避免依赖state.filters
      const defaultParams = {
        pageNum: 1,
        pageSize: 20,
        sortBy: 'create_time',
        sortDirection: 'DESC'
      };
      const queryParams = { ...defaultParams, ...state.filters, ...params };
      console.log('调用用户API，参数:', queryParams);

      const response = await userManagementApi.getUsers(queryParams);
      console.log('用户API响应:', response);

      if (response.success && response.data) {
        // 适配后端返回的数据结构
        const backendData = response.data as any;
        console.log('后端返回的用户数据:', backendData);

        setState(prev => ({
          ...prev,
          users: backendData.users || [],
          pagination: {
            current: backendData.page || 1,
            pageSize: backendData.pageSize || 20,
            total: backendData.totalCount || 0
          },
          loading: false
        }));
        console.log('成功获取用户列表，用户数量:', backendData.users?.length || 0);
      } else {
        console.error('获取用户列表失败:', response.error);
        setState(prev => ({ ...prev, loading: false }));
      }
    } catch (error) {
      console.error('获取用户列表异常:', error);
      setState(prev => ({ ...prev, loading: false }));
    }
  }, [state.filters]);

  // 获取用户统计信息
  const fetchUserStats = useCallback(async () => {
    try {
      const stats = await userManagementApi.getUserStats();
      setUserStats(stats);
    } catch (error) {
      console.error('获取用户统计失败:', error);
    }
  }, []);

  // 初始加载
  useEffect(() => {
    console.log('用户管理页面初始化，开始获取数据...');
    fetchUsers();
    fetchUserStats();
  }, [fetchUsers]);

  // 刷新数据
  const handleRefresh = () => {
    fetchUsers();
    fetchUserStats();
  };

  // 搜索处理
  const handleSearch = () => {
    const searchParams = {
      keyword: filters.keyword,
      status: filters.status ? parseInt(filters.status) : undefined,
      pageNum: 1
    };
    fetchUsers(searchParams);
  };

  // 重置筛选
  const handleResetFilters = () => {
    setFilters({
      keyword: '',
      status: '',
      startDate: '',
      endDate: ''
    });
    fetchUsers({ pageNum: 1 });
  };

  // 封禁用户
  const handleBanUser = async (userId: number) => {
    const reason = prompt('请输入封禁原因:');
    if (!reason) return;

    try {
      const response = await userManagementApi.banUser(userId, { reason });
      if (response.success) {
        alert('用户封禁成功');
        fetchUsers();
      } else {
        alert(`封禁失败: ${response.error?.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('封禁用户失败:', error);
      alert('操作失败，请稍后重试');
    }
  };

  // 解封用户
  const handleUnbanUser = async (userId: number) => {
    if (!confirm('确定要解封这个用户吗？')) return;

    try {
      const response = await userManagementApi.unbanUser(userId);
      if (response.success) {
        alert('用户解封成功');
        fetchUsers();
      } else {
        alert(`解封失败: ${response.error?.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('解封用户失败:', error);
      alert('操作失败，请稍后重试');
    }
  };

  // 禁言用户
  const handleMuteUser = async (userId: number) => {
    const reason = prompt('请输入禁言原因:');
    if (!reason) return;

    try {
      const response = await userManagementApi.muteUser(userId, { reason });
      if (response.success) {
        alert('用户禁言成功');
        fetchUsers();
      } else {
        alert(`禁言失败: ${response.error?.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('禁言用户失败:', error);
      alert('操作失败，请稍后重试');
    }
  };

  // 解除禁言
  const handleUnmuteUser = async (userId: number) => {
    if (!confirm('确定要解除这个用户的禁言吗？')) return;

    try {
      const response = await userManagementApi.unmuteUser(userId);
      if (response.success) {
        alert('用户解除禁言成功');
        fetchUsers();
      } else {
        alert(`解除禁言失败: ${response.error?.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('解除禁言失败:', error);
      alert('操作失败，请稍后重试');
    }
  };

  // 分页处理
  const handlePageChange = (page: number) => {
    fetchUsers({ pageNum: page });
  };

  return (
    <AdminLayout>
      <div className="user-management-page">
        {/* 页面头部 */}
        <div className="page-header">
          <div className="page-title-section">
            <h1 className="page-title">用户管理</h1>
            <p className="page-subtitle">管理平台用户，进行封禁、禁言等操作</p>
          </div>
          <div className="page-actions">
            <button onClick={handleRefresh} className="refresh-btn" disabled={state.loading}>
              {state.loading ? '刷新中...' : '刷新数据'}
            </button>
          </div>
        </div>

        {/* 页面内容 */}
        <div className="page-content">
          {/* 用户统计 */}
          <div className="stats-container">
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#1890ff', marginBottom: '8px' }}>
                {userStats?.totalUsers || state.pagination.total || 0}
              </div>
              <div style={{ color: '#6b7280' }}>总用户数</div>
            </div>
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#52c41a', marginBottom: '8px' }}>
                {userStats?.activeUsers || state.users.filter(u => u.status === UserStatus.ACTIVE).length}
              </div>
              <div style={{ color: '#6b7280' }}>正常用户</div>
            </div>
            <div style={{
              background: 'white',
              padding: '24px',
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#f5222d', marginBottom: '8px' }}>
                {userStats?.bannedUsers || state.users.filter(u => u.status === UserStatus.BANNED).length}
              </div>
              <div style={{ color: '#6b7280' }}>封禁用户</div>
            </div>
            <div style={{
              background: 'white',
              padding: '24px',
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#f59e0b', marginBottom: '8px' }}>
                {userStats?.mutedUsers || state.users.filter(u => u.status === UserStatus.MUTED).length}
              </div>
              <div style={{ color: '#6b7280' }}>禁言用户</div>
            </div>
            <div style={{
              background: 'white',
              padding: '24px',
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#722ed1', marginBottom: '8px' }}>
                {userStats?.newUsersToday || 0}
              </div>
              <div style={{ color: '#6b7280' }}>今日新增</div>
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
                  placeholder="用户名或邮箱"
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
                  <option value="0">正常</option>
                  <option value="1">封禁</option>
                  <option value="2">禁言</option>
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

          {/* 用户列表 */}
          <div className="list-container">
            <div style={{ padding: '20px', borderBottom: '1px solid #e5e7eb' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>
                用户列表 ({state.users.length} 个用户)
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
                <p>加载用户数据中...</p>
              </div>
            ) : state.users.length === 0 ? (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <p>暂无用户数据</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ background: '#f9fafb' }}>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        用户信息
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        邮箱
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        状态
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        注册时间
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        最后登录
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        操作
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {state.users.map(user => (
                      <tr key={user.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                        <td style={{ padding: '12px' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                            <div style={{
                              width: '40px',
                              height: '40px',
                              borderRadius: '50%',
                              background: '#3b82f6',
                              color: 'white',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              fontWeight: '600'
                            }}>
                              {user.username.charAt(0).toUpperCase()}
                            </div>
                            <div>
                              <div style={{ fontWeight: '500', marginBottom: '2px' }}>
                                {user.username}
                              </div>
                              <div style={{ fontSize: '12px', color: '#6b7280' }}>
                                ID: {user.id}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ fontSize: '14px' }}>
                            {user.email || '-'}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <span style={{
                            padding: '4px 8px',
                            borderRadius: '4px',
                            fontSize: '12px',
                            fontWeight: '500',
                            background: `${UserStatusColors[user.status]}20`,
                            color: UserStatusColors[user.status]
                          }}>
                            {UserStatusLabels[user.status]}
                          </span>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ fontSize: '14px', color: '#6b7280' }}>
                            {formatDate(user.createTime, 'YYYY-MM-DD')}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ fontSize: '14px', color: '#6b7280' }}>
                            {user.lastLoginTime ? formatDate(user.lastLoginTime, 'YYYY-MM-DD HH:mm') : '从未登录'}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                            {user.status === UserStatus.ACTIVE && (
                              <>
                                <button
                                  onClick={() => handleBanUser(user.id)}
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
                                  封禁
                                </button>
                                <button
                                  onClick={() => handleMuteUser(user.id)}
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
                                  禁言
                                </button>
                              </>
                            )}
                            {user.status === UserStatus.BANNED && (
                              <button
                                onClick={() => handleUnbanUser(user.id)}
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
                                解封
                              </button>
                            )}
                            {user.status === UserStatus.MUTED && (
                              <button
                                onClick={() => handleUnmuteUser(user.id)}
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
                                解除禁言
                              </button>
                            )}
                            <button
                              onClick={() => alert(`查看用户 ${user.username} 的详细信息`)}
                              style={{
                                padding: '4px 8px',
                                fontSize: '12px',
                                border: '1px solid #6b7280',
                                background: 'white',
                                color: '#6b7280',
                                borderRadius: '4px',
                                cursor: 'pointer'
                              }}
                            >
                              详情
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

export default UserManagementPageFinal;
