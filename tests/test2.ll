; ModuleID = 'module'
source_filename = "module"

@a = global i32 0
@b = global i32 10
@c = global <2 x i32> zeroinitializer
@d = global <2 x i32> <i32 1, i32 2>

define i32 @f(i32 %0) {
f_entry:
  %pointer_k = alloca i32, align 4
  store i32 %0, i32* %pointer_k, align 4
  %k = load i32, i32* %pointer_k, align 4
  %add_res = add i32 %k, 1
  ret i32 %add_res
}

define i32 @main() {
main_entry:
  %b = load i32, i32* @b, align 4
  store i32 %b, i32* @a, align 4
  %a = load i32, i32* @a, align 4
  %b1 = load i32, i32* @b, align 4
  %add_res = add i32 %a, %b1
  %"d[0]" = load i32, i32* getelementptr (<2 x i32>, <2 x i32>* @d, i32 0, i32 0), align 4
  %0 = call i32 @f(i32 %"d[0]")
  %mul_res = mul i32 %add_res, %0
  store i32 %mul_res, i32* getelementptr (<2 x i32>, <2 x i32>* @c, i32 0, i32 0), align 4
  %"c[0]" = load i32, i32* getelementptr (<2 x i32>, <2 x i32>* @c, i32 0, i32 0), align 4
  ret i32 %"c[0]"
}
