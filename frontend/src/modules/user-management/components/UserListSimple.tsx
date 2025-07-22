import React from 'react';
import {
  User,
  UserStatus,
  UserStatusLabels,
  UserStatusColors
} from '../types/user-management';
import { formatDate } from '../../../shared/utils/format';

interface UserListSimpleProps {
  users: User[];
  loading: boolean;
}

const UserListSimple: React.FC<UserListSimpleProps> = ({
  users,
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
        <p>加载用户数据中...</p>
      </div>
    );
  }

  if (users.length === 0) {
    return (
      <div style={{ 
        background: 'white', 
        padding: '40px', 
        borderRadius: '8px',
        textAlign: 'center',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)'
      }}>
        <p>暂无用户数据</p>
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
          用户列表 ({users.length} 个用户)
        </h3>
      </div>
      
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
            {users.map(user => (
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
                  <div style={{ display: 'flex', gap: '8px' }}>
                    {user.status === UserStatus.ACTIVE && (
                      <>
                        <button style={{
                          padding: '4px 8px',
                          fontSize: '12px',
                          border: '1px solid #dc2626',
                          background: '#dc2626',
                          color: 'white',
                          borderRadius: '4px',
                          cursor: 'pointer'
                        }}>
                          封禁
                        </button>
                        <button style={{
                          padding: '4px 8px',
                          fontSize: '12px',
                          border: '1px solid #f59e0b',
                          background: '#f59e0b',
                          color: 'white',
                          borderRadius: '4px',
                          cursor: 'pointer'
                        }}>
                          禁言
                        </button>
                      </>
                    )}
                    {user.status === UserStatus.BANNED && (
                      <button style={{
                        padding: '4px 8px',
                        fontSize: '12px',
                        border: '1px solid #10b981',
                        background: '#10b981',
                        color: 'white',
                        borderRadius: '4px',
                        cursor: 'pointer'
                      }}>
                        解封
                      </button>
                    )}
                    {user.status === UserStatus.MUTED && (
                      <button style={{
                        padding: '4px 8px',
                        fontSize: '12px',
                        border: '1px solid #10b981',
                        background: '#10b981',
                        color: 'white',
                        borderRadius: '4px',
                        cursor: 'pointer'
                      }}>
                        解除禁言
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

export default UserListSimple;
