; ModuleID = 'module'
source_filename = "module"

@a = global i32 10

define i32 @main() {
main_entry:
  %a = load i32, i32* @a, align 4
  %icmp_res = icmp ne i32 %a, 10
  %zext_res = zext i1 %icmp_res to i32
  %a1 = load i32, i32* @a, align 4
  %icmp_res2 = icmp sgt i32 %a1, 5
  %zext_res3 = zext i1 %icmp_res2 to i32
  %or_res = or i32 %zext_res, %zext_res3
  %a4 = load i32, i32* @a, align 4
  ret i32 %a4
}
