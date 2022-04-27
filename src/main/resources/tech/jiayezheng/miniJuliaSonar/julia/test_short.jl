# this is comment
# f(a, b=1) = a
# b = 1 + f(1 , 2)
# b, c = 1, 2
#
# g(ff) = ff(1)
#
# bar(a,b,x...) = (a,b,x)
#
# a = 1
# if (a == 0&&b==1)
#
# elseif a == 1
#     a = 2
# else
#     a = 3
# end
#
# array = [1,2,3,4,5]
#

f(a) = a

map(1:10) do x
    2x
end
#
# for e in 1:5
#     println(e)
# end
try
    open("/danger", "w") do f
        println(f, "Hello")
    end
catch
    @warn "Could not write file."
end