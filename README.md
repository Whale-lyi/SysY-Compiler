# Lab7实验报告

### 实验思路

1. 翻译while语句
1. 翻译break和continue语句

### ~~精巧的~~设计

- 增加两个全局变量 `whileNextStack, whileCondStack` 用来记录循环的层数，将 next 基本块和 condition 基本块压入栈
  - 访问 while 节点时，进入时压栈，结束时出栈

- 访问内部的 break 和 continue 节点时，通过出栈获取到基本块后，要再压回去，保证循环层数正确性


### 遇到的困难及解决办法

- 遇到了 `instruction expected to be numbered 'xxx'` 的报错
  - 在 stackoverflow 上找到了[相关解释](https://stackoverflow.com/questions/36094685/instruction-expected-to-be-numbered)

  - 解决方法就是在基本块的结束指令之后不要再添加其他指令了，经过测试，问题出现在 if 语句中，基本块结束语句后依然有跳转指令，并且产生了命名冲突
  
  - 通过增加 `boolean blockHasReturn` 变量来进行判断
  

