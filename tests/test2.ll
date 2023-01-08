; ModuleID = 'module'
source_filename = "module"

@a = global <2 x i32> <i32 1, i32 0>

define i32 @main() {
main_entry:
  %pointer_b = alloca <2 x i32>, align 8
  %GEP_0 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 0
  store i32 4, i32* %GEP_0, align 4
  %GEP_1 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 1
  store i32 0, i32* %GEP_1, align 4
  %"a[0]" = load i32, i32* getelementptr (<2 x i32>, <2 x i32>* @a, i32 0, i32 0), align 4
  %pointer_array = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 %"a[0]"
  store i32 6, i32* %pointer_array, align 4
  %pointer_array1 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 1
  store i32 7, i32* %pointer_array1, align 4
  %"a[0]2" = load i32, i32* getelementptr (<2 x i32>, <2 x i32>* @a, i32 0, i32 0), align 4
  %pointer_array3 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 1
  %"b[1]" = load i32, i32* %pointer_array3, align 4
  %icmp_res = icmp sgt i32 %"a[0]2", %"b[1]"
  %zext_res = zext i1 %icmp_res to i32
  %icmp_res4 = icmp ne i32 %zext_res, 0
  br i1 %icmp_res4, label %if_true, label %if_false

if_true:                                          ; preds = %main_entry
  %pointer_array5 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 0
  %"a[1]" = load i32, i32* getelementptr (<2 x i32>, <2 x i32>* @a, i32 0, i32 1), align 4
  store i32 %"a[1]", i32* %pointer_array5, align 4
  br label %next

if_false:                                         ; preds = %main_entry
  %pointer_array6 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 0
  %pointer_array7 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 1
  %"b[1]8" = load i32, i32* %pointer_array7, align 4
  store i32 %"b[1]8", i32* %pointer_array6, align 4
  br label %next

next:                                             ; preds = %if_false, %if_true
  %pointer_array9 = getelementptr <2 x i32>, <2 x i32>* %pointer_b, i32 0, i32 0
  %"b[0]" = load i32, i32* %pointer_array9, align 4
  ret i32 %"b[0]"
}
