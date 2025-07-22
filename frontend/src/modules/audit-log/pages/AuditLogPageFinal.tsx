import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../auth/contexts/AuthContext';
import AdminLayout from '../../../shared/components/AdminLayout';
import auditLogApi from '../services/auditLogApi';
import { formatDate } from '../../../shared/utils/format';
import {
  AuditLog,
  AuditLogQueryParams,
  AuditLogPageState,
  AuditLogStats as StatsType,
  AuditLogResult,
  AuditLogResultLabels,
  AuditLogResultColors
} from '../types/audit-log';
import '../../../shared/styles/admin-pages.css';

const AuditLogPageFinal: React.FC = () => {
  const { admin } = useAuth();
  
  // 页面状态
  const [state, setState] = useState<AuditLogPageState>({
    auditLogs: [],
    loading: false,
    filters: {
      page: 1,
      pageSize: 20,
      sortBy: 'create_time',
      sortOrder: 'DESC'
    },
    pagination: {
      current: 1,
      pageSize: 20,
      total: 0
    },
    detailModalVisible: false
  });

  // 统计信息
  const [stats, setStats] = useState<StatsType | null>(null);

  // 筛选状态
  const [filters, setFilters] = useState({
    action: '',
    targetType: '',
    result: '',
    adminUsername: '',
    startDate: '',
    endDate: ''
  });

  // 获取审计日志列表
  const fetchAuditLogs = useCallback(async (params?: Partial<AuditLogQueryParams>) => {
    setState(prev => ({ ...prev, loading: true }));
    
    try {
      const queryParams = { ...state.filters, ...params };
      const response = await auditLogApi.getAuditLogs(queryParams);
      
      if (response.success && response.data) {
        // 适配后端返回的数据结构
        const backendData = response.data as any;
        setState(prev => ({
          ...prev,
          auditLogs: backendData.list || [],
          pagination: {
            current: backendData.page || 1,
            pageSize: backendData.pageSize || 20,
            total: backendData.totalCount || 0
          },
          loading: false
        }));
        console.log('成功获取审计日志列表:', backendData);
      } else {
        console.error('获取审计日志列表失败:', response.error);
        setState(prev => ({ ...prev, loading: false }));
      }
    } catch (error) {
      console.error('获取审计日志列表异常:', error);
      setState(prev => ({ ...prev, loading: false }));
    }
  }, [state.filters]);

  // 获取统计信息
  const fetchStats = useCallback(async () => {
    try {
      const statsResponse = await auditLogApi.getAuditLogStats(7);
      if (statsResponse.success) {
        setStats(statsResponse.data);
      }
    } catch (error) {
      console.error('获取统计信息失败:', error);
    }
  }, []);

  // 初始加载
  useEffect(() => {
    fetchAuditLogs();
    fetchStats();
  }, []);

  // 刷新数据
  const handleRefresh = () => {
    fetchAuditLogs();
    fetchStats();
  };

  // 搜索处理
  const handleSearch = () => {
    const searchParams = {
      action: filters.action || undefined,
      targetType: filters.targetType || undefined,
      result: filters.result ? parseInt(filters.result) : undefined,
      adminUsername: filters.adminUsername || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
      page: 1
    };
    fetchAuditLogs(searchParams);
  };

  // 重置筛选
  const handleResetFilters = () => {
    setFilters({
      action: '',
      targetType: '',
      result: '',
      adminUsername: '',
      startDate: '',
      endDate: ''
    });
    fetchAuditLogs({ page: 1 });
  };

  // 导出日志
  const handleExport = async (format: 'csv' | 'excel') => {
    try {
      const response = await auditLogApi.exportAuditLogs(format, filters);
      if (response.success) {
        alert(`${format.toUpperCase()}格式的审计日志导出成功`);
      } else {
        alert(`导出失败: ${response.error?.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('导出审计日志失败:', error);
      alert('导出失败，请稍后重试');
    }
  };

  // 分页处理
  const handlePageChange = (page: number) => {
    fetchAuditLogs({ page });
  };

  // 获取操作描述
  const getActionDescription = (action: string): string => {
    const actionMap: Record<string, string> = {
      // 管理员相关操作
      'ADMIN_LOGIN': '管理员登录',
      'ADMIN_LOGOUT': '管理员登出',
      'ADMIN_CREATE': '创建管理员账户',
      'ADMIN_UPDATE': '更新管理员信息',
      'ADMIN_DELETE': '删除管理员账户',
      'ADMIN_ROLE_CHANGE': '修改管理员角色',
      'ADMIN_PASSWORD_RESET': '重置管理员密码',

      // 用户管理操作
      'USER_BAN': '封禁用户',
      'USER_UNBAN': '解封用户',
      'USER_MUTE': '禁言用户',
      'USER_UNMUTE': '解除禁言',
      'USER_DELETE': '删除用户',
      'USER_BATCH_BAN': '批量封禁用户',
      'USER_BATCH_MUTE': '批量禁言用户',
      'USER_PROFILE_UPDATE': '修改用户资料',

      // 商品管理操作
      'PRODUCT_APPROVE': '审核通过商品',
      'PRODUCT_REJECT': '审核拒绝商品',
      'PRODUCT_TAKEDOWN': '下架商品',
      'PRODUCT_DELETE': '删除商品',
      'PRODUCT_BATCH_APPROVE': '批量审核通过商品',
      'PRODUCT_BATCH_REJECT': '批量审核拒绝商品',
      'PRODUCT_BATCH_TAKEDOWN': '批量下架商品',

      // 订单管理操作
      'ORDER_CANCEL': '取消订单',
      'ORDER_REFUND': '订单退款',
      'ORDER_STATUS_CHANGE': '修改订单状态',

      // 系统配置操作
      'SYSTEM_CONFIG_UPDATE': '更新系统配置',
      'SYSTEM_MAINTENANCE': '系统维护',
      'SYSTEM_BACKUP': '系统备份',
      'SYSTEM_RESTORE': '系统恢复',

      // 数据操作
      'DATA_EXPORT': '数据导出',
      'DATA_IMPORT': '数据导入',
      'DATA_DELETE': '数据删除',

      // 安全相关操作
      'SECURITY_POLICY_UPDATE': '更新安全策略',
      'ACCESS_CONTROL_CHANGE': '修改访问控制',
      'PERMISSION_GRANT': '授予权限',
      'PERMISSION_REVOKE': '撤销权限',

      // 审计相关操作
      'AUDIT_LOG_VIEW': '查看审计日志',
      'AUDIT_LOG_EXPORT': '导出审计日志',

      // 其他敏感操作
      'SENSITIVE_DATA_ACCESS': '访问敏感数据',
      'BULK_OPERATION': '批量操作',
      'CRITICAL_FUNCTION_EXECUTE': '执行关键功能'
    };
    return actionMap[action] || action;
  };

  // 获取目标类型描述
  const getTargetTypeDescription = (targetType: string): string => {
    const targetTypeMap: Record<string, string> = {
      'ADMIN': '管理员',
      'USER': '用户',
      'PRODUCT': '商品',
      'ORDER': '订单',
      'CATEGORY': '分类',
      'SYSTEM': '系统',
      'CONFIG': '配置',
      'DATA': '数据',
      'PERMISSION': '权限',
      'ROLE': '角色',
      'AUDIT_LOG': '审计日志',
      'FILE': '文件',
      'BATCH': '批量操作'
    };
    return targetTypeMap[targetType] || targetType;
  };

  return (
    <AdminLayout>
      <div className="audit-log-page">
        {/* 页面头部 */}
        <div className="page-header">
          <div className="page-title-section">
            <h1 className="page-title">审计日志</h1>
            <p className="page-subtitle">查看系统操作日志，监控管理员行为</p>
          </div>
          <div className="page-actions">
            <button onClick={handleRefresh} className="refresh-btn" disabled={state.loading}>
              {state.loading ? '刷新中...' : '刷新数据'}
            </button>
            <button onClick={() => handleExport('csv')} className="export-btn">
              导出CSV
            </button>
            <button onClick={() => handleExport('excel')} className="export-btn">
              导出Excel
            </button>
          </div>
        </div>

        {/* 页面内容 */}
        <div className="page-content">
          {/* 统计信息 */}
          <div className="stats-container">
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#1890ff', marginBottom: '8px' }}>
                {stats?.totalLogs || state.pagination.total || 0}
              </div>
              <div style={{ color: '#6b7280' }}>总日志数</div>
            </div>
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#52c41a', marginBottom: '8px' }}>
                {stats?.successLogs || state.auditLogs.filter(log => log.result === AuditLogResult.SUCCESS).length}
              </div>
              <div style={{ color: '#6b7280' }}>成功操作</div>
            </div>
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#f5222d', marginBottom: '8px' }}>
                {stats?.failureLogs || state.auditLogs.filter(log => log.result === AuditLogResult.FAILURE).length}
              </div>
              <div style={{ color: '#6b7280' }}>失败操作</div>
            </div>
            <div style={{ 
              background: 'white', 
              padding: '24px', 
              borderRadius: '8px',
              boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
              textAlign: 'center'
            }}>
              <div style={{ fontSize: '32px', fontWeight: 'bold', color: '#722ed1', marginBottom: '8px' }}>
                {stats?.todayLogs || 0}
              </div>
              <div style={{ color: '#6b7280' }}>今日操作</div>
            </div>
          </div>

          {/* 筛选器 */}
          <div className="filters-container">
            <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', fontWeight: '600' }}>筛选条件</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '16px' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '4px', fontSize: '14px', fontWeight: '500' }}>操作类型</label>
                <select
                  value={filters.action}
                  onChange={(e) => setFilters(prev => ({ ...prev, action: e.target.value }))}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    border: '1px solid #d1d5db',
                    borderRadius: '6px',
                    fontSize: '14px'
                  }}
                >
                  <option value="">全部操作</option>
                  <optgroup label="管理员操作">
                    <option value="ADMIN_LOGIN">管理员登录</option>
                    <option value="ADMIN_LOGOUT">管理员登出</option>
                    <option value="ADMIN_CREATE">创建管理员账户</option>
                    <option value="ADMIN_UPDATE">更新管理员信息</option>
                    <option value="ADMIN_DELETE">删除管理员账户</option>
                  </optgroup>
                  <optgroup label="用户管理">
                    <option value="USER_BAN">封禁用户</option>
                    <option value="USER_UNBAN">解封用户</option>
                    <option value="USER_MUTE">禁言用户</option>
                    <option value="USER_UNMUTE">解除禁言</option>
                    <option value="USER_DELETE">删除用户</option>
                    <option value="USER_BATCH_BAN">批量封禁用户</option>
                    <option value="USER_BATCH_MUTE">批量禁言用户</option>
                  </optgroup>
                  <optgroup label="商品管理">
                    <option value="PRODUCT_APPROVE">审核通过商品</option>
                    <option value="PRODUCT_REJECT">审核拒绝商品</option>
                    <option value="PRODUCT_TAKEDOWN">下架商品</option>
                    <option value="PRODUCT_DELETE">删除商品</option>
                    <option value="PRODUCT_BATCH_APPROVE">批量审核通过商品</option>
                    <option value="PRODUCT_BATCH_REJECT">批量审核拒绝商品</option>
                    <option value="PRODUCT_BATCH_TAKEDOWN">批量下架商品</option>
                  </optgroup>
                  <optgroup label="系统操作">
                    <option value="SYSTEM_CONFIG_UPDATE">更新系统配置</option>
                    <option value="SYSTEM_MAINTENANCE">系统维护</option>
                    <option value="DATA_EXPORT">数据导出</option>
                    <option value="DATA_IMPORT">数据导入</option>
                  </optgroup>
                  <optgroup label="审计操作">
                    <option value="AUDIT_LOG_VIEW">查看审计日志</option>
                    <option value="AUDIT_LOG_EXPORT">导出审计日志</option>
                  </optgroup>
                </select>
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '4px', fontSize: '14px', fontWeight: '500' }}>目标类型</label>
                <select
                  value={filters.targetType}
                  onChange={(e) => setFilters(prev => ({ ...prev, targetType: e.target.value }))}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    border: '1px solid #d1d5db',
                    borderRadius: '6px',
                    fontSize: '14px'
                  }}
                >
                  <option value="">全部类型</option>
                  <option value="ADMIN">管理员</option>
                  <option value="USER">用户</option>
                  <option value="PRODUCT">商品</option>
                  <option value="ORDER">订单</option>
                  <option value="CATEGORY">分类</option>
                  <option value="SYSTEM">系统</option>
                  <option value="CONFIG">配置</option>
                  <option value="DATA">数据</option>
                  <option value="PERMISSION">权限</option>
                  <option value="ROLE">角色</option>
                  <option value="AUDIT_LOG">审计日志</option>
                  <option value="FILE">文件</option>
                  <option value="BATCH">批量操作</option>
                </select>
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '4px', fontSize: '14px', fontWeight: '500' }}>操作结果</label>
                <select
                  value={filters.result}
                  onChange={(e) => setFilters(prev => ({ ...prev, result: e.target.value }))}
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    border: '1px solid #d1d5db',
                    borderRadius: '6px',
                    fontSize: '14px'
                  }}
                >
                  <option value="">全部结果</option>
                  <option value="1">成功</option>
                  <option value="0">失败</option>
                </select>
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '4px', fontSize: '14px', fontWeight: '500' }}>操作人</label>
                <input
                  type="text"
                  value={filters.adminUsername}
                  onChange={(e) => setFilters(prev => ({ ...prev, adminUsername: e.target.value }))}
                  placeholder="管理员用户名"
                  style={{
                    width: '100%',
                    padding: '8px 12px',
                    border: '1px solid #d1d5db',
                    borderRadius: '6px',
                    fontSize: '14px'
                  }}
                />
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

          {/* 审计日志列表 */}
          <div className="list-container">
            <div style={{ padding: '20px', borderBottom: '1px solid #e5e7eb' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>
                审计日志 ({state.auditLogs.length} 条记录)
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
                <p>加载审计日志中...</p>
              </div>
            ) : state.auditLogs.length === 0 ? (
              <div style={{ padding: '40px', textAlign: 'center' }}>
                <p>暂无审计日志数据</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ background: '#f9fafb' }}>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        操作信息
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        目标对象
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        操作人
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        结果
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        IP地址
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        操作时间
                      </th>
                      <th style={{ padding: '12px', textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                        操作
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {state.auditLogs.map(log => (
                      <tr key={log.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                        <td style={{ padding: '12px' }}>
                          <div>
                            <div style={{ fontWeight: '500', marginBottom: '4px' }}>
                              {getActionDescription(log.action)}
                            </div>
                            {log.description && (
                              <div style={{ fontSize: '12px', color: '#6b7280' }}>
                                {log.description.length > 50
                                  ? `${log.description.substring(0, 50)}...`
                                  : log.description
                                }
                              </div>
                            )}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div>
                            <div style={{ fontSize: '14px', fontWeight: '500' }}>
                              {getTargetTypeDescription(log.targetType)}
                            </div>
                            {log.targetId && (
                              <div style={{ fontSize: '12px', color: '#6b7280' }}>
                                ID: {log.targetId}
                              </div>
                            )}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ fontSize: '14px' }}>
                            {log.adminUsername}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <span style={{
                            padding: '4px 8px',
                            borderRadius: '4px',
                            fontSize: '12px',
                            fontWeight: '500',
                            background: `${AuditLogResultColors[log.result]}20`,
                            color: AuditLogResultColors[log.result]
                          }}>
                            {AuditLogResultLabels[log.result]}
                          </span>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ fontSize: '14px', color: '#6b7280', fontFamily: 'monospace' }}>
                            {log.ipAddress}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <div style={{ fontSize: '14px', color: '#6b7280' }}>
                            {formatDate(log.createTime, 'YYYY-MM-DD HH:mm:ss')}
                          </div>
                        </td>
                        <td style={{ padding: '12px' }}>
                          <button
                            onClick={() => alert(`查看日志详情:\n操作: ${getActionDescription(log.action)}\n描述: ${log.description || '无'}\n结果: ${AuditLogResultLabels[log.result]}`)}
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

export default AuditLogPageFinal;
