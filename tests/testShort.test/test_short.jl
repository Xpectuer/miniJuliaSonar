# this is comment
f(a) = a


for a in 1:10
    a = a + 1
end
map(1:10) do x
    2x
end
#
# for e in 1:5
#     println(e)
# end

t = true
f = false
try
    open("/danger", "w") do f
        println(f, "Hello")
    end
catch
    @warn "Could not write file."
end