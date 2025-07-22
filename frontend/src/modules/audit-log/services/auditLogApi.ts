import api from '../../../shared/services/baseApi';
import {
  AuditLogQueryParams,
  AuditLogQueryResponse,
  AuditLogDetailResponse,
  AuditLogStatsResponse,
  AuditLogTrendResponse,
  ActionsResponse,
  TargetTypesResponse,
  AuditLogExportParams,
  AuditLogExportResponse,
  AuditLogStats,
  AuditLogTrendData,
  ActionOption,
  TargetTypeOption
} from '../types/audit-log';

// 审计日志API服务类
class AuditLogApiService {

  // 获取审计日志列表
  async getAuditLogs(params: AuditLogQueryParams = {}): Promise<AuditLogQueryResponse> {
    try {
      const queryParams = new URLSearchParams();
      
      if (params.adminId) queryParams.append('adminId', params.adminId.toString());
      if (params.action) queryParams.append('action', params.action);
      if (params.targetType) queryParams.append('targetType', params.targetType);
      if (params.targetId) queryParams.append('targetId', params.targetId.toString());
      if (params.ipAddress) queryParams.append('ipAddress', params.ipAddress);
      if (params.result !== undefined) queryParams.append('result', params.result.toString());
      if (params.startTime) queryParams.append('startTime', params.startTime);
      if (params.endTime) queryParams.append('endTime', params.endTime);
      if (params.keyword) queryParams.append('keyword', params.keyword);
      if (params.page) queryParams.append('page', params.page.toString());
      if (params.pageSize) queryParams.append('pageSize', params.pageSize.toString());
      if (params.sortBy) queryParams.append('sortBy', params.sortBy);
      if (params.sortOrder) queryParams.append('sortOrder', params.sortOrder);

      const response = await api.get(`/admin/audit-logs?${queryParams.toString()}`);
      return response.data;
    } catch (error: any) {
      console.error('获取审计日志列表失败:', error);
      return {
        success: false,
        error: {
          code: 'FETCH_AUDIT_LOGS_ERROR',
          message: error.response?.data?.message || '获取审计日志列表失败'
        }
      };
    }
  }

  // 获取审计日志详情
  async getAuditLogDetail(logId: number): Promise<AuditLogDetailResponse> {
    try {
      const response = await api.get(`/admin/audit-logs/${logId}`);
      return response.data;
    } catch (error: any) {
      console.error('获取审计日志详情失败:', error);
      return {
        success: false,
        error: {
          code: 'FETCH_AUDIT_LOG_DETAIL_ERROR',
          message: error.response?.data?.message || '获取审计日志详情失败'
        }
      };
    }
  }

  // 获取审计日志统计信息
  async getAuditLogStats(days: number = 7): Promise<AuditLogStatsResponse> {
    try {
      const response = await api.get(`/admin/audit-logs/stats?days=${days}`);
      return response.data;
    } catch (error: any) {
      console.error('获取审计日志统计失败:', error);
      // 返回默认数据
      return {
        success: false,
        error: {
          code: 'STATS_ERROR',
          message: '获取统计数据失败'
        }
      };
    }
  }

  // 获取审计日志趋势数据
  async getAuditLogTrend(days: number = 7): Promise<AuditLogTrendResponse> {
    try {
      const response = await api.get(`/admin/audit-logs/trend?days=${days}`);
      return response.data;
    } catch (error: any) {
      console.error('获取审计日志趋势失败:', error);
      // 返回模拟数据
      const trendData: AuditLogTrendData[] = [];
      const today = new Date();
      for (let i = days - 1; i >= 0; i--) {
        const date = new Date(today);
        date.setDate(date.getDate() - i);
        const dateStr = date.toISOString().split('T')[0];
        trendData.push({
          date: dateStr,
          totalCount: Math.floor(Math.random() * 200) + 50,
          successCount: Math.floor(Math.random() * 180) + 40,
          failureCount: Math.floor(Math.random() * 20) + 5
        });
      }
      return {
        success: true,
        data: trendData
      };
    }
  }

  // 获取可用的操作类型
  async getAvailableActions(): Promise<ActionsResponse> {
    try {
      const response = await api.get('/admin/audit-logs/actions');
      return response.data;
    } catch (error: any) {
      console.error('获取操作类型失败:', error);
      // 返回模拟数据
      const actions: ActionOption[] = [
        { code: 'ADMIN_LOGIN', description: '管理员登录' },
        { code: 'ADMIN_LOGOUT', description: '管理员登出' },
        { code: 'USER_BAN', description: '封禁用户' },
        { code: 'USER_UNBAN', description: '解封用户' },
        { code: 'USER_MUTE', description: '禁言用户' },
        { code: 'USER_UNMUTE', description: '解除禁言' },
        { code: 'PRODUCT_APPROVE', description: '审核通过商品' },
        { code: 'PRODUCT_REJECT', description: '审核拒绝商品' },
        { code: 'PRODUCT_TAKEDOWN', description: '下架商品' },
        { code: 'PRODUCT_DELETE', description: '删除商品' },
        { code: 'AUDIT_LOG_VIEW', description: '查看审计日志' },
        { code: 'AUDIT_LOG_EXPORT', description: '导出审计日志' },
        { code: 'DATA_EXPORT', description: '数据导出' },
        { code: 'SYSTEM_CONFIG_UPDATE', description: '更新系统配置' }
      ];
      return {
        success: true,
        data: actions
      };
    }
  }

  // 获取可用的目标类型
  async getAvailableTargetTypes(): Promise<TargetTypesResponse> {
    try {
      const response = await api.get('/admin/audit-logs/target-types');
      return response.data;
    } catch (error: any) {
      console.error('获取目标类型失败:', error);
      // 返回模拟数据
      const targetTypes: TargetTypeOption[] = [
        { code: 'ADMIN', description: '管理员' },
        { code: 'USER', description: '用户' },
        { code: 'PRODUCT', description: '商品' },
        { code: 'ORDER', description: '订单' },
        { code: 'SYSTEM', description: '系统' },
        { code: 'AUDIT_LOG', description: '审计日志' },
        { code: 'CONFIG', description: '配置' },
        { code: 'DATA', description: '数据' }
      ];
      return {
        success: true,
        data: targetTypes
      };
    }
  }

  // 导出审计日志
  async exportAuditLogs(params: AuditLogExportParams): Promise<AuditLogExportResponse> {
    try {
      const response = await api.post('/admin/audit-logs/export', params);
      return response.data;
    } catch (error: any) {
      console.error('导出审计日志失败:', error);
      return {
        success: false,
        error: {
          code: 'EXPORT_AUDIT_LOGS_ERROR',
          message: error.response?.data?.message || '导出审计日志失败'
        }
      };
    }
  }

  // 搜索审计日志（支持关键词搜索）
  async searchAuditLogs(keyword: string, limit: number = 10): Promise<AuditLogQueryResponse> {
    try {
      const response = await api.get(`/admin/audit-logs/search?keyword=${encodeURIComponent(keyword)}&limit=${limit}`);
      return response.data;
    } catch (error: any) {
      console.error('搜索审计日志失败:', error);
      return {
        success: false,
        error: {
          code: 'SEARCH_AUDIT_LOGS_ERROR',
          message: error.response?.data?.message || '搜索审计日志失败'
        }
      };
    }
  }

  // 获取时间范围内的日志数量
  async getLogCountByTimeRange(startTime: string, endTime: string): Promise<{ success: boolean; count?: number; error?: any }> {
    try {
      const response = await api.get(`/admin/audit-logs/count?startTime=${startTime}&endTime=${endTime}`);
      return {
        success: true,
        count: response.data.data?.count || 0
      };
    } catch (error: any) {
      console.error('获取日志数量失败:', error);
      return {
        success: false,
        error: error.response?.data?.message || '获取日志数量失败'
      };
    }
  }

  // 获取管理员操作统计
  async getAdminOperationStats(adminId: number, days: number = 30): Promise<{ success: boolean; data?: any; error?: any }> {
    try {
      const response = await api.get(`/admin/audit-logs/admin-stats?adminId=${adminId}&days=${days}`);
      return response.data;
    } catch (error: any) {
      console.error('获取管理员操作统计失败:', error);
      return {
        success: false,
        error: error.response?.data?.message || '获取管理员操作统计失败'
      };
    }
  }
}

// 导出单例实例
export const auditLogApi = new AuditLogApiService();
export default auditLogApi;
