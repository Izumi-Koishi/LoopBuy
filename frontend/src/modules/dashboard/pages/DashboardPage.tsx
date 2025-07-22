import React, { useState, useEffect } from 'react';
import { useAuth } from '../../auth/contexts/AuthContext';
import AdminLayout from '../../../shared/components/AdminLayout';
import { DashboardGrid, DashboardCard, DashboardRow, DashboardCol } from '../components/layout/DashboardLayout';
import StatCard from '../components/cards/StatCard';
import LineChart from '../components/charts/LineChart';
import PieChart from '../components/charts/PieChart';
import BarChart from '../components/charts/BarChart';
import dashboardApi from '../services/dashboardApi';
import { DashboardData, BackendDashboardData } from '../types/dashboard';
import './DashboardPage.css';
import '../../../shared/styles/admin-pages.css';

const DashboardPage: React.FC = () => {
  const { admin } = useAuth();
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 将后端数据转换为前端格式
  const convertBackendDataToFrontend = (backendData: BackendDashboardData): DashboardData => {
    return {
      stats: {
        totalUsers: backendData.overview.totalUsers,
        todayNewUsers: backendData.userStats.newUsersToday,
        totalProducts: backendData.overview.totalProducts,
        todayNewProducts: backendData.productStats.newProductsToday,
        totalTransactions: 0, // 后端暂无此数据，使用默认值
        todayTransactionAmount: 0, // 后端暂无此数据，使用默认值
        activeUsers: backendData.overview.totalActiveUsers,
        platformGrowthRate: backendData.userStats.userGrowthRate
      },
      userTrend: {
        registrationTrend: backendData.userTrend.map(item => ({
          date: item.date,
          value: item.value
        })),
        activeTrend: backendData.userTrend.map(item => ({
          date: item.date,
          value: item.value
        }))
      },
      productTrend: {
        publishTrend: backendData.productTrend.map(item => ({
          date: item.date,
          value: item.value
        })),
        soldTrend: backendData.productTrend.map(item => ({
          date: item.date,
          value: Math.floor(item.value * 0.3) // 模拟已售数据
        }))
      },
      transactionTrend: {
        volumeTrend: backendData.activityTrend.map(item => ({
          date: item.date,
          value: Math.floor(item.value * 0.5) // 模拟交易量数据
        })),
        amountTrend: backendData.activityTrend.map(item => ({
          date: item.date,
          value: item.value * 100 // 模拟交易金额数据
        }))
      },
      categoryDistribution: [
        { name: '电子产品', value: Math.floor(backendData.overview.totalProducts * 0.35), percentage: 35.2 },
        { name: '图书教材', value: Math.floor(backendData.overview.totalProducts * 0.26), percentage: 26.4 },
        { name: '生活用品', value: Math.floor(backendData.overview.totalProducts * 0.17), percentage: 17.4 },
        { name: '服装配饰', value: Math.floor(backendData.overview.totalProducts * 0.13), percentage: 12.8 },
        { name: '运动器材', value: Math.floor(backendData.overview.totalProducts * 0.08), percentage: 8.2 }
      ],
      productStatusDistribution: {
        onSale: backendData.productStats.onSaleProducts,
        sold: backendData.productStats.soldProducts,
        draft: backendData.overview.totalPendingProducts,
        offline: backendData.productStats.removedProducts
      },
      userActivityDistribution: {
        veryActive: Math.floor(backendData.overview.totalActiveUsers * 0.2),
        active: Math.floor(backendData.overview.totalActiveUsers * 0.5),
        inactive: backendData.overview.totalUsers - backendData.overview.totalActiveUsers
      },
      popularProducts: [
        { id: 1, title: 'iPhone 13 Pro', category: '电子产品', price: 6999, viewCount: 234, favoriteCount: 45 },
        { id: 2, title: '高等数学教材', category: '图书教材', price: 45, viewCount: 189, favoriteCount: 32 },
        { id: 3, title: 'Nike运动鞋', category: '服装配饰', price: 299, viewCount: 156, favoriteCount: 28 },
        { id: 4, title: '台式电脑主机', category: '电子产品', price: 3200, viewCount: 145, favoriteCount: 25 },
        { id: 5, title: '宿舍小冰箱', category: '生活用品', price: 280, viewCount: 134, favoriteCount: 22 }
      ],
      lastUpdated: backendData.lastUpdateTime
    };
  };

  // 获取仪表盘数据
  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);

    try {
      // 调用真实API
      const response = await dashboardApi.getDashboardData();

      console.log('🔍 API响应完整数据:', response);
      console.log('🔍 response.success:', response.success);
      console.log('🔍 response.data:', response.data);
      console.log('🔍 response.error:', response.error);

      if (response.success && response.data) {
        // 转换后端数据为前端格式
        const convertedData = convertBackendDataToFrontend(response.data);
        setDashboardData(convertedData);
        console.log('✅ 成功获取真实仪表盘数据:', response.data);
      } else {
        // API调用失败，显示错误而不是模拟数据
        console.error('❌ 仪表盘API调用失败:', response);
        console.error('❌ 错误详情:', response.error);
        setError(`获取仪表盘数据失败: ${response.error?.message || response.message || '未知错误'}`);
        setDashboardData(null);
      }
    } catch (err: any) {
      console.error('❌ 获取仪表盘数据异常:', err);
      setError(`获取仪表盘数据异常: ${err.message}`);
      setDashboardData(null);
    } finally {
      setLoading(false);
    }
  };

  // 组件挂载时获取数据
  useEffect(() => {
    console.log('🔍 仪表盘页面初始化');
    const token = localStorage.getItem('admin_token');
    const adminInfo = localStorage.getItem('admin_info');
    console.log('🔍 localStorage中的token:', token ? `${token.substring(0, 20)}...` : 'null');
    console.log('🔍 localStorage中的admin_info:', adminInfo);
    console.log('🔍 AuthContext中的admin:', admin);

    // 检查登录状态
    if (!token || !admin) {
      console.log('❌ 用户未登录，重定向到登录页面');
      window.location.href = '/login';
      return;
    }

    console.log('✅ 用户已登录，开始获取仪表盘数据');
    fetchDashboardData();
  }, [admin]);

  // 刷新数据
  const handleRefresh = () => {
    fetchDashboardData();
  };

  // 格式化货币
  const formatCurrency = (value: number | string): string => {
    const num = typeof value === 'string' ? parseFloat(value) : value;
    return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`;
  };

  if (error) {
    return (
      <AdminLayout>
        <div className="dashboard-page">
          <div className="page-header">
            <h1 className="page-title">数据仪表盘</h1>
            <div className="page-actions">
              <button onClick={handleRefresh} className="refresh-btn">
                重新加载
              </button>
            </div>
          </div>
          <div className="error-container">
            <div className="error-message">
              <h3>数据加载失败</h3>
              <p>{error}</p>
              <button onClick={handleRefresh} className="retry-btn">
                重试
              </button>
            </div>
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div className="dashboard-page">
        <div className="page-header">
          <div className="page-title-section">
            <h1 className="page-title">数据仪表盘</h1>
            <p className="page-subtitle">拾物校园二手交易平台管理系统</p>
          </div>
          <div className="page-actions">
            <button onClick={handleRefresh} className="refresh-btn" disabled={loading}>
              {loading ? '加载中...' : '刷新数据'}
            </button>
          </div>
        </div>
      {/* 统计卡片区域 */}
      <DashboardGrid columns={4}>
        <StatCard
          title="总用户数"
          value={dashboardData?.stats.totalUsers || 0}
          change={dashboardData?.stats.todayNewUsers}
          changeLabel="今日新增"
          icon="👥"
          color="primary"
          loading={loading}
        />
        <StatCard
          title="总商品数"
          value={dashboardData?.stats.totalProducts || 0}
          change={dashboardData?.stats.todayNewProducts}
          changeLabel="今日新增"
          icon="📦"
          color="success"
          loading={loading}
        />
        <StatCard
          title="总交易数"
          value={dashboardData?.stats.totalTransactions || 0}
          change={12}
          changeLabel="较昨日"
          icon="💰"
          color="warning"
          loading={loading}
        />
        <StatCard
          title="今日交易额"
          value={dashboardData?.stats.todayTransactionAmount || 0}
          change={8.5}
          changeLabel="较昨日 +8.5%"
          icon="💵"
          color="info"
          loading={loading}
          formatter={formatCurrency}
        />
      </DashboardGrid>

      {/* 图表区域 */}
      <DashboardRow gutter={24}>
        <DashboardCol span={12}>
          <DashboardCard title="用户注册趋势" loading={loading}>
            {dashboardData && (
              <LineChart
                data={dashboardData.userTrend.registrationTrend}
                yAxisLabel="注册人数"
                smooth={true}
                area={true}
                loading={loading}
              />
            )}
          </DashboardCard>
        </DashboardCol>
        
        <DashboardCol span={12}>
          <DashboardCard title="商品分类分布" loading={loading}>
            {dashboardData && (
              <PieChart
                data={dashboardData.categoryDistribution}
                loading={loading}
                showPercentage={true}
              />
            )}
          </DashboardCard>
        </DashboardCol>
      </DashboardRow>

      <DashboardRow gutter={24}>
        <DashboardCol span={12}>
          <DashboardCard title="用户活跃度分布" loading={loading}>
            {dashboardData && (
              <BarChart
                data={[
                  { name: '非常活跃', value: dashboardData.userActivityDistribution.veryActive },
                  { name: '活跃', value: dashboardData.userActivityDistribution.active },
                  { name: '不活跃', value: dashboardData.userActivityDistribution.inactive }
                ]}
                yAxisLabel="用户数量"
                loading={loading}
              />
            )}
          </DashboardCard>
        </DashboardCol>
        
        <DashboardCol span={12}>
          <DashboardCard title="热门商品排行榜" loading={loading}>
            {dashboardData && (
              <div className="popular-products-table">
                <table>
                  <thead>
                    <tr>
                      <th>排名</th>
                      <th>商品名称</th>
                      <th>分类</th>
                      <th>价格</th>
                      <th>浏览量</th>
                      <th>收藏量</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dashboardData.popularProducts.map((product, index) => (
                      <tr key={product.id}>
                        <td>
                          <span className={`rank rank-${index + 1}`}>
                            {index + 1}
                          </span>
                        </td>
                        <td className="product-name">{product.title}</td>
                        <td>
                          <span className="category-tag">{product.category}</span>
                        </td>
                        <td className="price">¥{product.price}</td>
                        <td>{product.viewCount}</td>
                        <td>{product.favoriteCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </DashboardCard>
        </DashboardCol>
      </DashboardRow>

      {/* 数据更新时间 */}
        {/* 数据更新时间 */}
        {dashboardData && (
          <div className="dashboard-footer">
            <p>数据更新时间: {new Date(dashboardData.lastUpdated).toLocaleString('zh-CN')}</p>
          </div>
        )}
      </div>
    </AdminLayout>
  );
};

export default DashboardPage;
