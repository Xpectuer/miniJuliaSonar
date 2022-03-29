using CSTParser
using CSTParser: EXPR
using JSON
using TOML

# 圣诞树🎄
mutable struct DecoratedAST
    julia_sonar_node_type::Any
    value::Any
    has_args::Bool
    args::Array{DecoratedAST}
    # WARNING: May not accurate for recursive expressions
    _start::Int
    _end::Int
end

macro TRIVIAL()
    -1
end

function build_dst(cst)
    function build_dst_recursive(cst, offset)
        if cst.args === nothing
            # println(offset, ":", offset + cst.span, "\t", CSTParser.valof(cst))
            dst = DecoratedAST(cst.head, cst.val,false ,[], offset, offset + cst.span)
            offset += cst.fullspan
            #println("\t"* string(dst.value))
            return dst, offset
        else
            dst = DecoratedAST(cst.head, cst.val, true,[], @TRIVIAL, @TRIVIAL)
            for a in cst
                child, offset = build_dst_recursive(a, offset)
                push!(dst.args, child)
            end

            dst._start = dst.args[1]._start
            dst._end = dst.args[1]._end

            #println(dst.value)
            return dst, offset
        end
    end

    root = DecoratedAST(:ROOT, "ROOT", true,[],@TRIVIAL,@TRIVIAL)
    offset = 1
    for a in cst
        child, offset = build_dst_recursive(a, offset)
        push!(root.args,child)
    end

    return root
end





# ==================================================
# ||                 UNITILITIES                  ||
# ==================================================
function fix_string_span_unicode!(e::EXPR)
    val = CSTParser.valof(e)
    if typeof(val) == String
        # Hacky length fix
        diff = sizeof(val) - length(val)
        e.span -= diff
        e.fullspan -= diff
    end
end

# print function for experiment
function print_cst(cst)
    function print_cst_recursive(cst, offset)
        if cst.args === nothing
            println(offset, ":", offset + cst.span, "\t", CSTParser.valof(cst))
            offset += cst.fullspan
        else
            for a in cst
                offset = print_cst_recursive(a, offset)
            end
        end

        return offset
    end

    print_cst_recursive(cst, 1)
end

# Override json function
function json(dst::DecoratedAST)
    io = IOBuffer()

    function walk(dst::DecoratedAST)
        write(io, "{")
        for fieldname in fieldnames(DecoratedAST)
            field = getfield(dst, fieldname)
            type = fieldtype(DecoratedAST, fieldname)
            if type == Array{DecoratedAST}
                write(io,"\"")
                write(io,fieldname)
                write(io,"\"")
                write(io,":")
                write(io,"[")

                for arg in field
                    walk(arg)
                end

                write(io,"]")
                write(io,",")
            elseif type == Bool || type == Int
                write(io,"\"")
                write(io, fieldname)
                write(io,"\"")
                write(io,":")
                write(io,string(field))
                write(io,",")
            else
                write(io,"\"")
                write(io, fieldname)
                write(io,"\"")
                write(io,":")
                write(io,"\"")
                write(io,string(field))
                write(io,"\"")
                write(io,",")

            end
        end
        write(io, "}")
        write(io,",")
    end

    walk(dst)

    ret = String(take!(io))
    return SubString(ret,1,length(ret) - 1)
end



# ==================================================
# ||                 ENTRYPOINT                   ||
# ==================================================
function dump_json(filename, output , endmark)
    try
        src = open(io -> read(io, String), filename)
        # println(src)
        cst = CSTParser.parse("$src")
        # print_cst(cst)

        dst = build_dst(cst)
        out = open(output * "_decorated.json", "w")
        write(out, json(dst))
        println("$output written !!!")
        close(out)
    finally
        _end = open(endmark, "w")
        close(_end)
    end
end

dump_json("test1.jl", "test1", "\$")