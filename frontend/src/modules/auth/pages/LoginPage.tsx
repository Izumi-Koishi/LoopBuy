import React, { useState, useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './LoginPage.css';

const LoginPage: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [validationErrors, setValidationErrors] = useState<{username?: string, password?: string}>({});
  const { admin, login, isLoading, error } = useAuth();

  // 清除验证错误
  useEffect(() => {
    if (username || password) {
      setValidationErrors({});
    }
  }, [username, password]);

  // 如果已经登录，重定向到仪表板
  if (admin) {
    return <Navigate to="/dashboard" replace />;
  }

  // 表单验证函数
  const validateForm = (): boolean => {
    const errors: {username?: string, password?: string} = {};

    if (!username.trim()) {
      errors.username = '请输入用户名';
    } else if (username.trim().length < 2) {
      errors.username = '用户名至少需要2个字符';
    }

    if (!password.trim()) {
      errors.password = '请输入密码';
    } else if (password.length < 6) {
      errors.password = '密码至少需要6个字符';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // 处理表单提交
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 表单验证
    if (!validateForm()) {
      return;
    }

    // 调用登录函数
    const success = await login(username.trim(), password);
    if (success) {
      // 登录成功会自动重定向到仪表板
      console.log('登录成功');
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div className="login-header">
          <h1>Shiwu管理后台</h1>
          <p>校园二手交易平台管理系统</p>
        </div>
        
        <form className="login-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">用户名</label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="请输入管理员用户名"
              disabled={isLoading}
              autoComplete="username"
              className={validationErrors.username ? 'error' : ''}
            />
            {validationErrors.username && (
              <span className="field-error">{validationErrors.username}</span>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="password">密码</label>
            <div className="password-input">
              <input
                type={showPassword ? 'text' : 'password'}
                id="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="请输入密码"
                disabled={isLoading}
                autoComplete="current-password"
                className={validationErrors.password ? 'error' : ''}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                disabled={isLoading}
                aria-label={showPassword ? '隐藏密码' : '显示密码'}
              >
                {showPassword ? '👁️' : '👁️‍🗨️'}
              </button>
            </div>
            {validationErrors.password && (
              <span className="field-error">{validationErrors.password}</span>
            )}
          </div>
          
          {error && (
            <div className="error-message">
              {error}
            </div>
          )}
          
          <button
            type="submit"
            className="login-button"
            disabled={isLoading}
          >
            {isLoading ? '登录中...' : '登录'}
          </button>
        </form>
        
        <div className="login-footer">
          <p>© 2024 Shiwu校园二手交易平台</p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
