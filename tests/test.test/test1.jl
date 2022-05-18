hh() = 1

module mod
    a = 0
    function g(a, b)
        while a < 10
            a = a + 1
        end
        return a
    end
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
        deadbeef = 'n'
        deadbeef1 = (3 < 1);

        i = 0
        while 1 < i < 10
            i+=1
        end

        for j in [1, 2, 3, 4]
            i+=1
        end

        a = b = c = d = 2
        a = a - b * (-c + -d)
        a = a ⊽ b
        strs = """
            dsadsadasdsadsadsa
            1232132132132132131
            """
        long_line = "str\
                        123213213"
        t = a != b && a == b

        deadbeef = a + -+-+-1
        b
    end

    h(a) = 2a
    #(h ∘ g ∘ f)(a + 2)
    #g ∘ f(2)
    g(1,2)
end


l = [1, 2, 3, 4]
l[1]

abstract type MBase <: Real end
primitive type Intersting <: Number 40 end
mutable struct Test <: MBase
    a :: Int
    b :: Char
end

test = Test(1,'2')
test.a.b.a.b.a.b

pair = 1 => 2

deadbeef, beef1 = 1, 1

b = 2
function bar()
    v :: Pair{T,S} where T
    v1 :: Pair{T,S} where {T,S}
    v2 :: Pair{T,S} where {T <: Real,S <: Real}
    return 1
end
bar()

ff = 123.123213
ff1 = 123.1

 f = open("/danger", "w")
 try
        println(f, "Hello")
    catch e
        error("Could not write file.")
    finally
        close(f)
    end

a = missing