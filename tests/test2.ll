; ModuleID = 'module'
source_filename = "module"

@a = global <2 x i32> <i32 1, i32 0>

define i32 @main() {
main_entry:
  %pointer_b = alloca <2 x i32>, align 8
  %GEP_0 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 0
  store i32 4, i32* %GEP_0, align 4
  %GEP_1 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 1
  store i32 6, i32* %GEP_1, align 4
  %"a[0]" = load i32, i32* getelementptr (<2 x i32>, <2 x i32>* @a, i32 0, i32 0), align 4
  %icmp_res = icmp ne i32 %"a[0]", 0
  br i1 %icmp_res, label %if_true, label %if_false

if_true:                                          ; preds = %main_entry
  %"pointer_array[0]" = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 0
  %"a[1]" = load i32, i32* getelementptr (<2 x i32>, <2 x i32>* @a, i32 0, i32 1), align 4
  store i32 %"a[1]", i32* %"pointer_array[0]", align 4
  br label %next

if_false:                                         ; preds = %main_entry
  %"pointer_array[0]1" = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 0
  %"a[0]2" = load i32, i32* getelementptr (<2 x i32>, <2 x i32>* @a, i32 0, i32 0), align 4
  store i32 %"a[0]2", i32* %"pointer_array[0]1", align 4
  br label %next

next:                                             ; preds = %if_false, %if_true
  %"pointer_array[0]3" = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 0
  %"b[0]" = load i32, i32* %"pointer_array[0]3", align 4
  ret i32 %"b[0]"
}
