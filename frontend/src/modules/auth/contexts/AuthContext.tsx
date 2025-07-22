import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { AdminVO, AuthContextType } from '../types/auth';
import authApi from '../services/authApi';

// 创建认证上下文
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// 认证提供者组件的props类型
interface AuthProviderProps {
  children: ReactNode;
}

// 认证提供者组件
export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [admin, setAdmin] = useState<AdminVO | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 组件挂载时检查本地存储的登录状态
  useEffect(() => {
    const token = localStorage.getItem('admin_token');
    const adminInfo = localStorage.getItem('admin_info');
    
    if (token && adminInfo) {
      try {
        const parsedAdmin = JSON.parse(adminInfo);
        setAdmin(parsedAdmin);
      } catch (error) {
        // 如果解析失败，清除本地存储
        localStorage.removeItem('admin_token');
        localStorage.removeItem('admin_info');
      }
    }
  }, []);

  // 登录函数
  const login = async (username: string, password: string): Promise<boolean> => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await authApi.adminLogin({ username, password });

      if (response.success && response.data) {
        // 登录成功，保存用户信息和token
        setAdmin(response.data);
        localStorage.setItem('admin_token', response.data.token);
        localStorage.setItem('admin_info', JSON.stringify(response.data));
        return true;
      } else {
        // 登录失败，设置错误信息
        const errorMessage = response.error?.userTip || response.error?.message || '登录失败';
        setError(errorMessage);
        return false;
      }
    } catch (error: any) {
      console.error('登录错误:', error);
      setError('网络连接失败，请稍后重试');
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  // 登出函数
  const logout = async () => {
    setAdmin(null);
    setError(null);
    await authApi.adminLogout();
  };

  const value: AuthContextType = {
    admin,
    login,
    logout,
    isLoading,
    error,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

// 使用认证上下文的Hook
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
