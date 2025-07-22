import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../auth/contexts/AuthContext';
import AdminLayout from '../../../shared/components/AdminLayout';
import UserFilters from '../components/UserFilters';
import UserList from '../components/UserList';
import UserActionModal from '../components/UserActionModal';
import userManagementApi from '../services/userManagementApi';
import {
  User,
  UserQueryParams,
  UserAction,
  UserActionConfigs,
  BatchUserOperationParams,
  UserManagementState,
  UserStats
} from '../types/user-management';
import './UserManagementPage.css';
import '../../../shared/styles/admin-pages.css';

const UserManagementPage: React.FC = () => {
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

  // 获取用户列表
  const fetchUsers = useCallback(async (params?: Partial<UserQueryParams>) => {
    setState(prev => ({ ...prev, loading: true }));
    
    try {
      const queryParams = { ...state.filters, ...params };
      const response = await userManagementApi.getUsers(queryParams);
      
      if (response.success && response.data) {
        setState(prev => ({
          ...prev,
          users: response.data!.users,
          pagination: {
            current: response.data!.pagination.pageNum,
            pageSize: response.data!.pagination.pageSize,
            total: response.data!.pagination.total
          },
          loading: false
        }));
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
    fetchUsers();
    fetchUserStats();
  }, []);

  // 处理筛选条件变化
  const handleFiltersChange = (newFilters: UserQueryParams) => {
    setState(prev => ({
      ...prev,
      filters: { ...newFilters, pageNum: 1 },
      selectedUserIds: []
    }));
    fetchUsers({ ...newFilters, pageNum: 1 });
  };

  // 重置筛选条件
  const handleFiltersReset = () => {
    const resetFilters: UserQueryParams = {
      pageNum: 1,
      pageSize: 20,
      sortBy: 'create_time',
      sortDirection: 'DESC'
    };
    setState(prev => ({
      ...prev,
      filters: resetFilters,
      selectedUserIds: []
    }));
    fetchUsers(resetFilters);
  };

  // 处理选择变化
  const handleSelectionChange = (selectedIds: number[]) => {
    setState(prev => ({ ...prev, selectedUserIds: selectedIds }));
  };

  // 处理排序
  const handleSort = (sortBy: string, sortDirection: 'ASC' | 'DESC') => {
    const newFilters = { ...state.filters, sortBy, sortDirection };
    setState(prev => ({ ...prev, filters: newFilters }));
    fetchUsers(newFilters);
  };

  // 处理分页
  const handlePageChange = (page: number, pageSize: number) => {
    const newFilters = { ...state.filters, pageNum: page, pageSize };
    setState(prev => ({ ...prev, filters: newFilters }));
    fetchUsers(newFilters);
  };

  // 处理用户操作
  const handleUserAction = (action: UserAction, userIds: number[]) => {
    setState(prev => ({
      ...prev,
      currentAction: action,
      selectedUserIds: userIds,
      actionModalVisible: true
    }));
  };

  // 处理批量操作
  const handleBatchAction = (action: UserAction) => {
    if (state.selectedUserIds.length === 0) {
      alert('请先选择要操作的用户');
      return;
    }
    handleUserAction(action, state.selectedUserIds);
  };

  // 确认操作
  const handleConfirmAction = async (params: BatchUserOperationParams) => {
    if (!state.currentAction) return;

    setState(prev => ({ ...prev, loading: true }));

    try {
      const response = await userManagementApi.batchOperation(state.currentAction, params);
      
      if (response.success) {
        // 操作成功，刷新列表和统计
        await Promise.all([fetchUsers(), fetchUserStats()]);
        setState(prev => ({
          ...prev,
          actionModalVisible: false,
          selectedUserIds: [],
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
      actionModalVisible: false,
      currentAction: undefined
    }));
  };

  // 刷新数据
  const handleRefresh = () => {
    fetchUsers();
    fetchUserStats();
  };

  // 获取选中用户的用户名
  const getSelectedUsernames = (): string[] => {
    return state.selectedUserIds.map(id => {
      const user = state.users.find(u => u.id === id);
      return user?.username || '';
    });
  };

  // 渲染统计卡片
  const renderStatsCards = () => {
    if (!userStats) return null;

    const statsData = [
      { label: '总用户数', value: userStats.totalUsers, color: '#1890ff' },
      { label: '正常用户', value: userStats.activeUsers, color: '#52c41a' },
      { label: '封禁用户', value: userStats.bannedUsers, color: '#f5222d' },
      { label: '禁言用户', value: userStats.mutedUsers, color: '#faad14' },
      { label: '今日新增', value: userStats.newUsersToday, color: '#722ed1' },
      { label: '本月新增', value: userStats.newUsersThisMonth, color: '#13c2c2' }
    ];

    return (
      <div className="stats-cards">
        {statsData.map(stat => (
          <div key={stat.label} className="stat-card">
            <div className="stat-value" style={{ color: stat.color }}>
              {stat.value.toLocaleString()}
            </div>
            <div className="stat-label">{stat.label}</div>
          </div>
        ))}
      </div>
    );
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
        {/* 统计卡片 */}
        {renderStatsCards()}

        {/* 筛选器 */}
        <UserFilters
          filters={state.filters}
          onFiltersChange={handleFiltersChange}
          onReset={handleFiltersReset}
        />

        {/* 批量操作栏 */}
        {state.selectedUserIds.length > 0 && (
          <div className="batch-actions">
            <div className="batch-info">
              已选择 {state.selectedUserIds.length} 个用户
            </div>
            <div className="batch-buttons">
              <button
                onClick={() => handleBatchAction(UserAction.BAN)}
                className="batch-btn ban-btn"
              >
                <span className="btn-icon">🚫</span>
                批量封禁
              </button>
              <button
                onClick={() => handleBatchAction(UserAction.UNBAN)}
                className="batch-btn unban-btn"
              >
                <span className="btn-icon">✅</span>
                批量解封
              </button>
              <button
                onClick={() => handleBatchAction(UserAction.MUTE)}
                className="batch-btn mute-btn"
              >
                <span className="btn-icon">🔇</span>
                批量禁言
              </button>
              <button
                onClick={() => handleBatchAction(UserAction.UNMUTE)}
                className="batch-btn unmute-btn"
              >
                <span className="btn-icon">🔊</span>
                批量解除禁言
              </button>
            </div>
          </div>
        )}

        {/* 用户列表 */}
        <UserList
          users={state.users}
          loading={state.loading}
          selectedUserIds={state.selectedUserIds}
          onSelectionChange={handleSelectionChange}
          onUserAction={handleUserAction}
          onSort={handleSort}
          pagination={state.pagination}
          onPageChange={handlePageChange}
        />
      </div>

      {/* 操作模态框 */}
      {state.currentAction && (
        <UserActionModal
          visible={state.actionModalVisible}
          action={state.currentAction}
          userIds={state.selectedUserIds}
          usernames={getSelectedUsernames()}
          onConfirm={handleConfirmAction}
          onCancel={handleCancelAction}
          loading={state.loading}
        />
      )}
      </div>
    </AdminLayout>
  );
};

export default UserManagementPage;
