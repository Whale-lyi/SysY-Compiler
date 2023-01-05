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
  %0 = call i32 @f(i32 1)
  ret i32 %0
}
