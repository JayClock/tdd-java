#### 组件构造
- 无需构造的组件——组件实例
- 如果注册的组件不可实例化，则抛出异常
    - [x] 抽象类
    - [x] 接口
- 构造函数注入
    - [x] 无依赖的组件应该通过默认构造函数生成组件实例
    - [x] 有依赖的组件，通过 Inject 标注的构造函数生成组件实例
    - [x] 如果所依赖的组件也存在依赖，那么需要对所依赖的组件也完成依赖注入
    - [x] 如果组件有多于一个 Inject 标注的构造函数，则抛出异常
    - [x] 如果组件没有 Inject 标注的构造函数，也没有默认构造函数
    - [x] 如果组件需要的依赖不存在，则抛出异常
    - [x] 如果组件间存在循环依赖，则抛出异常
- 字段注入
    - [x] 通过 Inject 标注将字段声明为依赖组件
    - [x] 如果字段为 final 则抛出异常
    - [x] 依赖中应包含 Inject Field 声明的依赖
- 方法注入
    - [x] 通过 Inject 标注的方法，其参数为依赖组件
    - [x] 通过 Inject 标注的无参数方法，会被调用
    - [x] 按照子类中的规则，覆盖父类中的 Inject 方法
    - [x] 如果方法定义类型参数，则抛出异常
    - [x] 依赖中应包含 Inject Method 声明的依赖
#### 依赖选择
- 对 Provider 类型的依赖
    - [x] 可从容器中获取依赖的 Provider
    - [x] 注入构造函数中可以声明对于 Provider 的依赖
    - [x] 注入字段中可以声明对于 Provider 的依赖
    - [x] 注入方法中可声明对于 Provider 的依赖
    - [x] 将构造函数中的 Provider 加入依赖
    - [x] 将字段中的 Provider 加入依赖
    - [x] 将方法中的 Provider 加入依赖
- 自定义 Qualifier 的依赖
    - 注册组件时，可额外指定 Qualifier
      - [x] 针对 instance 指定一个 Qualifier
      - [x] 针对组件指定一个 Qualifier
      - [x] 针对instance 指定多个 Qualifier
      - [x] 针对组件指定多个 Qualifier
    - 注册组件时，如果不是合法的 Qualifier，则不接受组件注册
    - 寻找依赖时，需同时满足类型与自定义 Qualifier 标注
      - [x] 在检查依赖时使用 Qualifier
      - [x] 在检查循环依赖时使用 Qualifier
      - 构造函数注入可以使用 Qualifier 声明依赖
        - [x] 依赖中包含 Qualifier
        - [x] 如果不是合法的 Qualifier，则组件非法
      - 字段注入可以使用 Qualifier 声明依赖
        - [x] 依赖中包含 Qualifier 
        - [x] 如果不是合法的 Qualifier，则组件非法
      - 函数注入可以使用 Qualifier 声明依赖
        - [x] 依赖中包含 Qualifier
        - [x] 如果不是合法的 Qualifier，则组件非法
#### 生命周期管理
- Singleton 生命周期
    - [x] 注册组件时，可额外指定是否为 Singleton
    - [x] 注册包含 Qualifier 的组件时，可额外指定是否为 Singleton
    - [x] 注册组件时，可从类对象上提取 Singleton 标注
    - [x] 容器组件默认不是 Single 生命周期
    - [x] 包含 Qualifier 的组件默认不是 Single 生命周期
    - [ ] 对于包含 Scope 的组件，检测依赖关系
- 自定义 Scope 标注
    - [] 可向容器注册自定义 Scope 标注的回调