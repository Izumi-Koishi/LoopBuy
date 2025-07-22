import React from 'react';
import {
  AuditLog,
  AuditLogResult,
  AuditLogResultLabels,
  AuditLogResultColors
} from '../types/audit-log';
import { formatDate } from '../../../shared/utils/format';

interface AuditLogListSimpleProps {
  auditLogs: AuditLog[];
  loading: boolean;
}

const AuditLogListSimple: React.FC<AuditLogListSimpleProps> = ({
  auditLogs,
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
        <p>加载审计日志中...</p>
      </div>
    );
  }

  if (auditLogs.length === 0) {
    return (
      <div style={{ 
        background: 'white', 
        padding: '40px', 
        borderRadius: '8px',
        textAlign: 'center',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
      }}>
        <p>暂无审计日志数据</p>
      </div>
    );
  }

  const getActionDescription = (action: string): string => {
    const actionMap: Record<string, string> = {
      'ADMIN_LOGIN': '管理员登录',
      'ADMIN_LOGOUT': '管理员登出',
      'USER_BAN': '封禁用户',
      'USER_UNBAN': '解封用户',
      'USER_MUTE': '禁言用户',
      'USER_UNMUTE': '解除禁言',
      'PRODUCT_APPROVE': '审核通过商品',
      'PRODUCT_REJECT': '审核拒绝商品',
      'PRODUCT_TAKEDOWN': '下架商品',
      'PRODUCT_DELETE': '删除商品',
      'AUDIT_LOG_VIEW': '查看审计日志',
      'AUDIT_LOG_EXPORT': '导出审计日志'
    };
    return actionMap[action] || action;
  };

  const getTargetTypeDescription = (targetType: string): string => {
    const targetTypeMap: Record<string, string> = {
      'ADMIN': '管理员',
      'USER': '用户',
      'PRODUCT': '商品',
      'ORDER': '订单',
      'SYSTEM': '系统',
      'AUDIT_LOG': '审计日志'
    };
    return targetTypeMap[targetType] || targetType;
  };

  return (
    <div style={{ 
      background: 'white', 
      borderRadius: '8px',
      boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
      overflow: 'hidden'
    }}>
      <div style={{ padding: '20px', borderBottom: '1px solid #e5e7eb' }}>
        <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>
          审计日志 ({auditLogs.length} 条记录)
        </h3>
      </div>
      
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
            {auditLogs.map(log => (
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
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default AuditLogListSimple;
