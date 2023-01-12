; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
main_entry:
  %pointer_a = alloca i32, align 4
  store i32 0, i32* %pointer_a, align 4
  %pointer_count = alloca i32, align 4
  store i32 0, i32* %pointer_count, align 4
  br label %while_condition

while_condition:                                  ; preds = %main_entry
  %a = load i32, i32* %pointer_a, align 4
  %icmp_res = icmp sle i32 %a, 0
  %zext_res = zext i1 %icmp_res to i32
  %icmp_res1 = icmp ne i32 %zext_res, 0
  br i1 %icmp_res1, label %while_body, label %next

while_body:                                       ; preds = %while_condition
  %a2 = load i32, i32* %pointer_a, align 4
  %sub_res = sub i32 %a2, 1
  store i32 %sub_res, i32* %pointer_a, align 4
  ret i32 1

next:                                             ; preds = %while_condition
  %count = load i32, i32* %pointer_count, align 4
  ret i32 %count
}
