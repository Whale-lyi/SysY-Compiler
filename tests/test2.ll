; ModuleID = 'module'
source_filename = "module"

@a = global i32 10

define i32 @main() {
main_entry:
  %pointer_i = alloca i32, align 4
  store i32 0, i32* %pointer_i, align 4
  %pointer_j = alloca i32, align 4
  store i32 0, i32* %pointer_j, align 4
  br label %while_condition

while_condition:                                  ; preds = %next22, %if_true20, %main_entry
  %j = load i32, i32* %pointer_j, align 4
  %icmp_res = icmp slt i32 %j, 5
  %zext_res = zext i1 %icmp_res to i32
  %icmp_res1 = icmp ne i32 %zext_res, 0
  br i1 %icmp_res1, label %while_body, label %next

while_body:                                       ; preds = %while_condition
  %j2 = load i32, i32* %pointer_j, align 4
  %add_res = add i32 %j2, 1
  store i32 %add_res, i32* %pointer_j, align 4
  br label %while_condition3

next:                                             ; preds = %while_condition
  %i23 = load i32, i32* %pointer_i, align 4
  %j24 = load i32, i32* %pointer_j, align 4
  %add_res25 = add i32 %i23, %j24
  store i32 %add_res25, i32* @a, align 4
  %a = load i32, i32* @a, align 4
  ret i32 %a

while_condition3:                                 ; preds = %next13, %while_body
  %i = load i32, i32* %pointer_i, align 4
  %icmp_res6 = icmp slt i32 %i, 5
  %zext_res7 = zext i1 %icmp_res6 to i32
  %icmp_res8 = icmp ne i32 %zext_res7, 0
  br i1 %icmp_res8, label %while_body4, label %next5

while_body4:                                      ; preds = %while_condition3
  %i9 = load i32, i32* %pointer_i, align 4
  %icmp_res10 = icmp sge i32 %i9, 3
  %zext_res11 = zext i1 %icmp_res10 to i32
  %icmp_res12 = icmp ne i32 %zext_res11, 0
  br i1 %icmp_res12, label %if_true, label %if_false

next5:                                            ; preds = %if_true, %while_condition3
  %j16 = load i32, i32* %pointer_j, align 4
  %icmp_res17 = icmp eq i32 %j16, 3
  %zext_res18 = zext i1 %icmp_res17 to i32
  %icmp_res19 = icmp ne i32 %zext_res18, 0
  br i1 %icmp_res19, label %if_true20, label %if_false21

if_true:                                          ; preds = %while_body4
  br label %next5
  br label %next13

if_false:                                         ; preds = %while_body4
  br label %next13

next13:                                           ; preds = %if_false, %if_true
  %i14 = load i32, i32* %pointer_i, align 4
  %add_res15 = add i32 %i14, 1
  store i32 %add_res15, i32* %pointer_i, align 4
  br label %while_condition3

if_true20:                                        ; preds = %next5
  br label %while_condition
  br label %next22

if_false21:                                       ; preds = %next5
  br label %next22

next22:                                           ; preds = %if_false21, %if_true20
  br label %while_condition
}
