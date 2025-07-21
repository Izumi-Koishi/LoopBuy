# Shiwu校园二手交易平台 - 后端API

## 📋 API文档

完整的API文档请查看：**[API_Documentation.md](./API_Documentation.md)**

## 🚀 快速开始

### 启动服务器

```bash
# 编译项目
mvn clean compile

# 运行服务器
mvn exec:java -Dexec.mainClass="com.shiwu.Application"
```

### 访问地址

- **API基础地址**: http://localhost:8080
- **API文档**: [API_Documentation.md](./API_Documentation.md)

## 📊 API概览

### 核心模块

| 模块 | 基础路径 | 描述 |
|------|---------|------|
| 用户模块 | `/api/user/` | 用户注册、登录、资料管理、关注功能 |
| 商品模块 | `/api/products/` | 商品发布、查询、编辑、图片上传 |
| 分类模块 | `/api/categories/` | 商品分类管理 |
| 消息模块 | `/api/messages/` | 私信聊天、会话管理 |
| 订单模块 | `/api/orders/` | 订单创建、状态管理、发货收货 |
| 购物车模块 | `/api/cart/` | 购物车商品管理 |
| 评价模块 | `/api/reviews/` | 商品评价、评分统计 |
| 支付模块 | `/api/payments/` | 支付创建、处理、状态查询 |

### 管理员模块

| 模块 | 基础路径 | 描述 |
|------|---------|------|
| 管理员认证 | `/api/admin/login` | 管理员登录 |
| 仪表盘 | `/api/admin/dashboard` | 统计数据展示 |
| 用户管理 | `/api/admin/users/` | 用户查询、封禁管理 |
| 商品管理 | `/api/admin/products/` | 商品审核、下架管理 |
| 审计日志 | `/api/admin/audit-logs/` | 系统操作日志查询 |
| 支付超时管理 | `/api/payment-timeout/` | 支付超时检查和处理 |

## 🔐 认证方式

### JWT Token认证

大部分API需要在请求头中携带JWT Token：

```http
Authorization: Bearer {your_jwt_token}
```

### 获取Token

1. **用户Token**: 通过 `POST /api/user/login` 获取
2. **管理员Token**: 通过 `POST /api/admin/login` 获取

## 📝 响应格式

所有API都遵循统一的响应格式：

```json
{
  "success": true,
  "data": {},
  "error": {
    "code": "错误代码",
    "message": "错误信息",
    "userTip": "用户提示"
  },
  "message": "响应消息"
}
```

## 🧪 测试

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -Dtest="*ControllerTest"
```

### 测试覆盖率

- **总测试用例**: 351个
- **测试通过率**: 100% ✅
- **模块覆盖率**: 100%

## 📁 项目结构

```
backend/
├── src/main/java/com/shiwu/
│   ├── user/           # 用户模块
│   ├── product/        # 商品模块
│   ├── message/        # 消息模块
│   ├── order/          # 订单模块
│   ├── cart/           # 购物车模块
│   ├── review/         # 评价模块
│   ├── payment/        # 支付模块
│   ├── admin/          # 管理员模块
│   └── common/         # 公共工具类
├── src/test/java/      # 测试代码
├── API_Documentation.md # 完整API文档
└── README.md          # 本文件
```

## 🔧 开发工具

- **Java**: 8+
- **Maven**: 3.6+
- **Servlet**: 3.1
- **数据库**: MySQL 8.0
- **测试框架**: JUnit 5 + Mockito

## 📞 技术支持

如有问题，请查看：
1. **完整API文档**: [API_Documentation.md](./API_Documentation.md)
2. **测试用例**: `src/test/java/` 目录
3. **错误代码对照表**: API文档附录A

---

**版本**: v1.0  
**最后更新**: 2023-12-20
