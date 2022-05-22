f = (a, b)-> a * b

function g(f, a, b)
    f(a,b)
end

g(f,"1","2")