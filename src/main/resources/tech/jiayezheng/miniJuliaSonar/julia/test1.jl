module mod
    a= 0
    function f(a)
        if a == 0
            b = 2
        elseif a == 1
            b = 2.2 + b
        elseif a > 2
            b = 2 * b
        else
            b = true
        end
        a = "nm\$l"

        i = 0
        while i < 10
            i+=1
        end

        for j in 1:10
            i+=1
        end

        b
    end
    f(a)
end