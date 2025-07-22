// 仪表盘数据类型定义

// 总览统计数据 - 与后端DashboardStatsVO.OverviewStats对齐
export interface OverviewStats {
  totalUsers: number;
  totalProducts: number;
  totalActiveUsers: number;
  totalPendingProducts: number;
  averageRating: number;
  totalFollowRelations: number;
}

// 用户统计数据 - 与后端DashboardStatsVO.UserStats对齐
export interface UserStats {
  newUsersToday: number;
  newUsersThisWeek: number;
  newUsersThisMonth: number;
  bannedUsers: number;
  mutedUsers: number;
  userGrowthRate: number;
}

// 商品统计数据 - 与后端DashboardStatsVO.ProductStats对齐
export interface ProductStats {
  newProductsToday: number;
  newProductsThisWeek: number;
  newProductsThisMonth: number;
  onSaleProducts: number;
  soldProducts: number;
  removedProducts: number;
  productGrowthRate: number;
}

// 系统活动统计数据 - 与后端DashboardStatsVO.ActivityStats对齐
export interface ActivityStats {
  adminLoginCount: number;
  auditLogCount: number;
  userLoginCount: number;
  systemErrors: number;
  systemUptime: number;
}

// 核心统计指标（保持向后兼容）
export interface DashboardStats {
  totalUsers: number;
  todayNewUsers: number;
  totalProducts: number;
  todayNewProducts: number;
  totalTransactions: number;
  todayTransactionAmount: number;
  activeUsers: number;
  platformGrowthRate: number;
}

// 趋势数据点
export interface TrendDataPoint {
  date: string;
  value: number;
}

// 用户统计趋势
export interface UserTrend {
  registrationTrend: TrendDataPoint[];
  activeTrend: TrendDataPoint[];
}

// 商品统计趋势
export interface ProductTrend {
  publishTrend: TrendDataPoint[];
  soldTrend: TrendDataPoint[];
}

// 交易统计趋势
export interface TransactionTrend {
  volumeTrend: TrendDataPoint[];
  amountTrend: TrendDataPoint[];
}

// 分类分布数据
export interface CategoryDistribution {
  name: string;
  value: number;
  percentage: number;
}

// 商品状态分布
export interface ProductStatusDistribution {
  onSale: number;
  sold: number;
  draft: number;
  offline: number;
}

// 用户活跃度分布
export interface UserActivityDistribution {
  veryActive: number;    // 非常活跃（近7天有活动）
  active: number;        // 活跃（近30天有活动）
  inactive: number;      // 不活跃（30天以上无活动）
}

// 热门商品数据
export interface PopularProduct {
  id: number;
  title: string;
  category: string;
  price: number;
  viewCount: number;
  favoriteCount: number;
}

// 仪表盘完整数据
export interface DashboardData {
  stats: DashboardStats;
  userTrend: UserTrend;
  productTrend: ProductTrend;
  transactionTrend: TransactionTrend;
  categoryDistribution: CategoryDistribution[];
  productStatusDistribution: ProductStatusDistribution;
  userActivityDistribution: UserActivityDistribution;
  popularProducts: PopularProduct[];
  lastUpdated: string;
}

// 后端返回的趋势数据 - 与后端DashboardStatsVO.TrendData对齐
export interface BackendTrendData {
  date: string;
  value: number;
  label?: string;
}

// 后端返回的完整仪表盘数据 - 与后端DashboardStatsVO对齐
export interface BackendDashboardData {
  overview: {
    totalUsers: number;
    totalProducts: number;
    totalActiveUsers: number;
    totalPendingProducts: number;
    averageRating: number;
    totalFollowRelations: number;
  };
  userStats: {
    newUsersToday: number;
    newUsersThisWeek: number;
    newUsersThisMonth: number;
    bannedUsers: number;
    mutedUsers: number;
    userGrowthRate: number;
  };
  productStats: {
    newProductsToday: number;
    newProductsThisWeek: number;
    newProductsThisMonth: number;
    onSaleProducts: number;
    soldProducts: number;
    removedProducts: number;
    productGrowthRate: number;
  };
  activityStats: {
    adminLoginCount: number;
    auditLogCount: number;
    userLoginCount: number;
    systemErrors: number;
    systemUptime: number;
  };
  userTrend: BackendTrendData[];
  productTrend: BackendTrendData[];
  activityTrend: BackendTrendData[];
  lastUpdateTime: string;
}

// API响应类型 - 与后端API格式对齐
export interface DashboardResponse {
  success: boolean;
  data?: BackendDashboardData;
  error?: {
    code: string;
    message: string;
    userTip?: string;
  };
  message?: string;
}

// 图表配置类型
export interface ChartConfig {
  title?: string;
  height?: number;
  showLegend?: boolean;
  showGrid?: boolean;
  colors?: string[];
}

// ECharts选项类型（简化版）
export interface EChartsOption {
  title?: any;
  tooltip?: any;
  legend?: any;
  grid?: any;
  xAxis?: any;
  yAxis?: any;
  series?: any[];
  color?: string[];
}
