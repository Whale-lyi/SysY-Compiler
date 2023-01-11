; ModuleID = 'module'
source_filename = "module"

@a = global i32 10

define i32 @main() {
main_entry:
  %pointer_i = alloca i32, align 4
  store i32 0, i32* %pointer_i, align 4
  br label %while_condition

while_condition:                                  ; preds = %while_body, %main_entry
  %i = load i32, i32* %pointer_i, align 4
  %icmp_res = icmp slt i32 %i, 5
  %zext_res = zext i1 %icmp_res to i32
  %icmp_res1 = icmp ne i32 %zext_res, 0
  br i1 %icmp_res1, label %while_body, label %next

while_body:                                       ; preds = %while_condition
  %i2 = load i32, i32* %pointer_i, align 4
  %add_res = add i32 %i2, 1
  store i32 %add_res, i32* %pointer_i, align 4
  br label %while_condition

next:                                             ; preds = %while_condition
  %i3 = load i32, i32* %pointer_i, align 4
  store i32 %i3, i32* @a, align 4
  %a = load i32, i32* @a, align 4
  ret i32 %a
}
