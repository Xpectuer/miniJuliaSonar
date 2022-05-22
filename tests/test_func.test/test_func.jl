struct A
    a
    b
    c
end

function foo(x,y,z)
    a = A(x,y,z)
end

foo(1,2,'3')