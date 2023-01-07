; ModuleID = 'module'
source_filename = "module"

@a = global i32 10

define i32 @main() {
main_entry:
  %a = load i32, i32* @a, align 4
  %icmp_res = icmp sgt i32 %a, 5
  %zext_res = zext i1 %icmp_res to i32
  %icmp_res1 = icmp ne i32 %zext_res, 0
  br i1 %icmp_res1, label %if_true, label %if_false

if_true:                                          ; preds = %main_entry
  %a2 = load i32, i32* @a, align 4
  %icmp_res3 = icmp sgt i32 %a2, 8
  %zext_res4 = zext i1 %icmp_res3 to i32
  %icmp_res5 = icmp ne i32 %zext_res4, 0
  br i1 %icmp_res5, label %if_true6, label %if_false7

if_false:                                         ; preds = %main_entry
  store i32 20, i32* @a, align 4
  br label %next

next:                                             ; preds = %if_false, %next8
  %a9 = load i32, i32* @a, align 4
  ret i32 %a9

if_true6:                                         ; preds = %if_true
  store i32 2, i32* @a, align 4
  br label %next8

if_false7:                                        ; preds = %if_true
  store i32 10, i32* @a, align 4
  br label %next8

next8:                                            ; preds = %if_false7, %if_true6
  br label %next
}
