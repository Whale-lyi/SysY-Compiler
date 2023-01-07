; ModuleID = 'module'
source_filename = "module"

@a = global <3 x i32> <i32 0, i32 1, i32 1>

define i32 @main() {
main_entry:
  %"a[0]" = load i32, i32* getelementptr (<3 x i32>, <3 x i32>* @a, i32 0, i32 0), align 4
  %icmp_res = icmp ne i32 0, %"a[0]"
  %xor_res = xor i1 %icmp_res, true
  %zext_res = zext i1 %xor_res to i32
  store i32 %zext_res, i32* getelementptr (<3 x i32>, <3 x i32>* @a, i32 0, i32 0), align 4
  %"a[0]1" = load i32, i32* getelementptr (<3 x i32>, <3 x i32>* @a, i32 0, i32 0), align 4
  %icmp_res2 = icmp ne i32 %"a[0]1", 0
  br i1 %icmp_res2, label %if_true, label %if_false

if_true:                                          ; preds = %main_entry
  store i32 2, i32* getelementptr (<3 x i32>, <3 x i32>* @a, i32 0, i32 1), align 4
  br label %next

if_false:                                         ; preds = %main_entry
  br label %next

next:                                             ; preds = %if_false, %if_true
  %"a[1]" = load i32, i32* getelementptr (<3 x i32>, <3 x i32>* @a, i32 0, i32 1), align 4
  ret i32 %"a[1]"
}
