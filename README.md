# Lab4实验报告

### 实验思路

1. 在创建的 MyIRVisitor 的构造函数中初始化 LLVM, 并将写入文件的路径作为参数传递给 visitor
2. 为需要翻译的文件创建模块，将 `moudle builder` `i32Type`作为Visitor的成员变量
3. 进入 `funcDef` 时为 `module` 添加函数，并为函数添加基本块
4. 访问到 `return` 语句时使用 `IRBuilder` 在基本块内生成生成`ret`指令
5. 访问表达式运算时使用 `IRBuilder` 在基本块内生成能够实现该运算的指令

### ~~精巧的~~设计

- 使用 Visitor 来遍历，将 `LLVMValueRef` 作为每个节点的返回值

### 遇到的困难及解决办法

- 对于某个运算不清楚应该使用 LLVM 提供的哪个方法
  - 使用 Clang 生成中间代码来查看应该如何实现，再查阅手册找到对应函数

