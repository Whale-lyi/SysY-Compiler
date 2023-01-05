; ModuleID = 'module'
source_filename = "module"

define i32 @f(i32 %0) {
fEntry:
  %pointer_i = alloca i32, align 4
  store i32 %0, i32* %pointer_i, align 4
  %i = load i32, i32* %pointer_i, align 4
  ret i32 %i
}

define i32 @main() {
mainEntry:
  %pointer_a = alloca i32, align 4
  store i32 1, i32* %pointer_a, align 4
  %a = load i32, i32* %pointer_a, align 4
  %0 = call i32 @f(i32 %a)
  ret i32 %0
}
