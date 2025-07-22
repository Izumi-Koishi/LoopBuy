import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../modules/auth/contexts/AuthContext';
import './AdminLayout.css';

interface AdminLayoutProps {
  children: React.ReactNode;
}

interface MenuItem {
  key: string;
  path: string;
  icon: string;
  label: string;
  description?: string;
}

const AdminLayout: React.FC<AdminLayoutProps> = ({ children }) => {
  const { admin, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // 菜单项配置
  const menuItems: MenuItem[] = [
    {
      key: 'dashboard',
      path: '/dashboard',
      icon: '📊',
      label: '数据仪表盘',
      description: '查看平台核心数据和统计信息'
    },
    {
      key: 'products',
      path: '/products',
      icon: '📦',
      label: '商品管理',
      description: '审核和管理平台商品'
    },
    {
      key: 'users',
      path: '/users',
      icon: '👥',
      label: '用户管理',
      description: '管理用户账户和权限'
    },
    {
      key: 'audit-logs',
      path: '/audit-logs',
      icon: '📋',
      label: '审计日志',
      description: '查看系统操作记录'
    }
  ];

  // 获取当前激活的菜单项
  const getActiveMenuItem = (): MenuItem | undefined => {
    return menuItems.find(item => location.pathname.startsWith(item.path));
  };

  // 处理登出
  const handleLogout = async () => {
    if (window.confirm('确定要退出登录吗？')) {
      await logout();
      navigate('/login');
    }
  };

  // 切换侧边栏折叠状态
  const toggleSidebar = () => {
    setSidebarCollapsed(!sidebarCollapsed);
  };

  const activeMenuItem = getActiveMenuItem();

  return (
    <div className={`admin-layout ${sidebarCollapsed ? 'sidebar-collapsed' : ''}`}>
      {/* 侧边栏 */}
      <div className="admin-sidebar">
        <div className="sidebar-header">
          <div className="logo">
            <span className="logo-icon">🛒</span>
            {!sidebarCollapsed && <span className="logo-text">拾物管理后台</span>}
          </div>
          <button 
            className="sidebar-toggle"
            onClick={toggleSidebar}
            title={sidebarCollapsed ? '展开侧边栏' : '折叠侧边栏'}
          >
            {sidebarCollapsed ? '→' : '←'}
          </button>
        </div>

        <nav className="sidebar-nav">
          <ul className="nav-menu">
            {menuItems.map(item => (
              <li key={item.key} className="nav-item">
                <Link
                  to={item.path}
                  className={`nav-link ${location.pathname.startsWith(item.path) ? 'active' : ''}`}
                  title={sidebarCollapsed ? item.label : ''}
                >
                  <span className="nav-icon">{item.icon}</span>
                  {!sidebarCollapsed && (
                    <div className="nav-content">
                      <span className="nav-label">{item.label}</span>
                      {item.description && (
                        <span className="nav-description">{item.description}</span>
                      )}
                    </div>
                  )}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div className="sidebar-footer">
          <div className="admin-info">
            <div className="admin-avatar">
              <span>{admin?.username?.charAt(0).toUpperCase()}</span>
            </div>
            {!sidebarCollapsed && (
              <div className="admin-details">
                <div className="admin-name">{admin?.username}</div>
                <div className="admin-role">{admin?.role}</div>
              </div>
            )}
          </div>
          <button 
            className="logout-btn"
            onClick={handleLogout}
            title="退出登录"
          >
            <span className="logout-icon">🚪</span>
            {!sidebarCollapsed && <span>退出登录</span>}
          </button>
        </div>
      </div>

      {/* 主内容区域 */}
      <div className="admin-main">
        <div className="main-header">
          <div className="breadcrumb">
            <span className="breadcrumb-item">管理后台</span>
            {activeMenuItem && (
              <>
                <span className="breadcrumb-separator">/</span>
                <span className="breadcrumb-item active">{activeMenuItem.label}</span>
              </>
            )}
          </div>
          <div className="header-actions">
            <div className="current-time">
              {new Date().toLocaleString('zh-CN')}
            </div>
          </div>
        </div>

        <div className="main-content">
          {children}
        </div>
      </div>
    </div>
  );
};

export default AdminLayout;
