import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../auth/contexts/AuthContext';
import AdminLayout from '../../../shared/components/AdminLayout';
import AuditLogFilters from '../components/AuditLogFilters';
import AuditLogList from '../components/AuditLogList';
import AuditLogStats from '../components/AuditLogStats';
import AuditLogDetailModal from '../components/AuditLogDetailModal';
import auditLogApi from '../services/auditLogApi';
import {
  AuditLog,
  AuditLogQueryParams,
  AuditLogPageState,
  AuditLogStats as StatsType,
  AuditLogTrendData,
  ActionOption,
  TargetTypeOption
} from '../types/audit-log';
import './AuditLogPage.css';
import '../../../shared/styles/admin-pages.css';

const AuditLogPage: React.FC = () => {
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

  // 筛选选项
  const [filterOptions, setFilterOptions] = useState<{
    actions: ActionOption[];
    targetTypes: TargetTypeOption[];
  }>({
    actions: [],
    targetTypes: []
  });

  // 获取审计日志列表
  const fetchAuditLogs = useCallback(async (params?: Partial<AuditLogQueryParams>) => {
    setState(prev => ({ ...prev, loading: true }));
    
    try {
      const queryParams = { ...state.filters, ...params };
      const response = await auditLogApi.getAuditLogs(queryParams);
      
      if (response.success && response.data) {
        setState(prev => ({
          ...prev,
          auditLogs: response.data!.auditLogs,
          pagination: {
            current: response.data!.pagination.page,
            pageSize: response.data!.pagination.pageSize,
            total: response.data!.pagination.total
          },
          loading: false
        }));
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
      const [statsResponse, trendResponse] = await Promise.all([
        auditLogApi.getAuditLogStats(7),
        auditLogApi.getAuditLogTrend(7)
      ]);
      
      setState(prev => ({
        ...prev,
        stats: statsResponse.success ? statsResponse.data : undefined,
        trendData: trendResponse.success ? trendResponse.data : undefined
      }));
    } catch (error) {
      console.error('获取统计信息失败:', error);
    }
  }, []);

  // 获取筛选选项
  const fetchFilterOptions = useCallback(async () => {
    try {
      const [actionsResponse, targetTypesResponse] = await Promise.all([
        auditLogApi.getAvailableActions(),
        auditLogApi.getAvailableTargetTypes()
      ]);
      
      setFilterOptions({
        actions: actionsResponse.success ? actionsResponse.data || [] : [],
        targetTypes: targetTypesResponse.success ? targetTypesResponse.data || [] : []
      });
    } catch (error) {
      console.error('获取筛选选项失败:', error);
    }
  }, []);

  // 初始加载
  useEffect(() => {
    fetchAuditLogs();
    fetchStats();
    fetchFilterOptions();
  }, []);

  // 处理筛选条件变化
  const handleFiltersChange = (newFilters: AuditLogQueryParams) => {
    setState(prev => ({
      ...prev,
      filters: { ...newFilters, page: 1 }
    }));
    fetchAuditLogs({ ...newFilters, page: 1 });
  };

  // 重置筛选条件
  const handleFiltersReset = () => {
    const resetFilters: AuditLogQueryParams = {
      page: 1,
      pageSize: 20,
      sortBy: 'create_time',
      sortOrder: 'DESC'
    };
    setState(prev => ({
      ...prev,
      filters: resetFilters
    }));
    fetchAuditLogs(resetFilters);
  };

  // 处理排序
  const handleSort = (sortBy: string, sortOrder: 'ASC' | 'DESC') => {
    const newFilters = { ...state.filters, sortBy, sortOrder };
    setState(prev => ({ ...prev, filters: newFilters }));
    fetchAuditLogs(newFilters);
  };

  // 处理分页
  const handlePageChange = (page: number, pageSize: number) => {
    const newFilters = { ...state.filters, page, pageSize };
    setState(prev => ({ ...prev, filters: newFilters }));
    fetchAuditLogs(newFilters);
  };

  // 查看详情
  const handleViewDetail = (log: AuditLog) => {
    setState(prev => ({
      ...prev,
      selectedLog: log,
      detailModalVisible: true
    }));
  };

  // 关闭详情模态框
  const handleCloseDetail = () => {
    setState(prev => ({
      ...prev,
      detailModalVisible: false,
      selectedLog: undefined
    }));
  };

  // 刷新数据
  const handleRefresh = () => {
    fetchAuditLogs();
    fetchStats();
  };

  // 导出日志
  const handleExport = async (format: 'csv' | 'excel' = 'csv') => {
    try {
      const response = await auditLogApi.exportAuditLogs({
        ...state.filters,
        format
      });
      
      if (response.success && response.data) {
        // 创建下载链接
        const link = document.createElement('a');
        link.href = response.data.downloadUrl;
        link.download = response.data.filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        alert('导出成功！');
      } else {
        alert(response.error?.message || '导出失败');
      }
    } catch (error) {
      alert('导出异常，请稍后重试');
    }
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
        <AuditLogStats
          stats={state.stats}
          trendData={state.trendData}
          loading={state.loading}
        />

        {/* 筛选器 */}
        <AuditLogFilters
          filters={state.filters}
          onFiltersChange={handleFiltersChange}
          onReset={handleFiltersReset}
          actions={filterOptions.actions}
          targetTypes={filterOptions.targetTypes}
        />

        {/* 日志列表 */}
        <AuditLogList
          auditLogs={state.auditLogs}
          loading={state.loading}
          onSort={handleSort}
          onViewDetail={handleViewDetail}
          pagination={state.pagination}
          onPageChange={handlePageChange}
        />
      </div>

      {/* 详情模态框 */}
      <AuditLogDetailModal
        visible={state.detailModalVisible}
        auditLog={state.selectedLog}
        onClose={handleCloseDetail}
      />
      </div>
    </AdminLayout>
  );
};

export default AuditLogPage;
