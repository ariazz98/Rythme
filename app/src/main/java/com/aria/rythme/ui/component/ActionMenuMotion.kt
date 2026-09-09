package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.*

/** 09-05 原片的归一化轨迹；开/关各自拟合，页面只提供源/目标边界。 */
internal object ActionMenuMotion {
    const val OpenMillis = 500
    const val CloseMillis = 300
    const val Width = 250f
    const val Corner = 34f

    fun smooth(p: Float, start: Float, end: Float): Float {
        val t = ((p - start) / (end - start)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** 保形 Hermite：中间节点共享切线，不在每个采样点反复刹停。 */
    internal fun curve(p: Float, vararg knots: Pair<Float, Float>): Float {
        if (p <= knots.first().first) return knots.first().second
        if (p >= knots.last().first) return knots.last().second
        fun slope(i: Int) = (knots[i+1].second-knots[i].second)/(knots[i+1].first-knots[i].first)
        fun tangent(i: Int): Float {
            if (i == 0) return slope(0)
            if (i == knots.lastIndex) return 0f
            val a=slope(i-1); val b=slope(i)
            if (a*b <= 0f) return 0f
            val before=knots[i].first-knots[i-1].first
            val after=knots[i+1].first-knots[i].first
            val w1=2f*after+before; val w2=after+2f*before
            return (w1+w2)/(w1/a+w2/b)
        }
        val i=(0 until knots.lastIndex).first { p <= knots[it+1].first }
        val span=knots[i+1].first-knots[i].first
        val t=(p-knots[i].first)/span
        return (2*t*t*t-3*t*t+1)*knots[i].second+(t*t*t-2*t*t+t)*span*tangent(i)+
            (-2*t*t*t+3*t*t)*knots[i+1].second+(t*t*t-t*t)*span*tangent(i+1)
    }

    fun openingFoldFraction(count: Int, pressedIndex: Int): Float {
        val remaining=(0 until count).filter { it != pressedIndex }
        return if (remaining.isEmpty()) .5f else remaining.map { (it+.5f)/count }.average().toFloat()
    }

    /** 关闭统一向整组胶囊中心收聚，不继承展开时“未点击图标”的位置。 */
    const val ClosingFoldFraction = .5f

    /**
     * 关闭时以实际绘制轮廓为起点。已铺开的菜单沿原关闭轨迹收回；还很小的表面
     * 连续过渡至源胶囊，不先放大成完整菜单，也不把展开轨迹压到 160ms 倒放。
     */
    fun closeFromSnapshot(
        start: MenuMorphGeometry, source: Rect, target: Rect,
        fraction: Float, closingFold: Float
    ): MenuMorphGeometry {
        val t = fraction.coerceIn(0f, 1f)
        if (t == 0f) return start
        val end = MenuMorphGeometry(source, source.height / 2f)
        if (t == 1f) return end
        val carry = 1f - smooth(t, 0f, .3f)
        val path = closingGeometry(source, start.body, t, start.bodyCorner, closingFold).let {
            it.copy(
                topCorner = it.topCorner + (start.topCorner - start.bodyCorner) * carry,
                skew = it.skew + start.skew * carry,
                neck = it.neck + start.neck * carry,
                headScale = it.headScale + (start.headScale - 1f) * carry,
                headDepth = mix(start.headDepth, it.headDepth, smooth(t, 0f, .15f))
            )
        }
        val expansion = ((start.body.height - source.height) /
            (target.height - source.height).coerceAtLeast(1f)).coerceIn(0f, 1f)
        return start.interpolate(end, smooth(t, 0f, 1f))
            .interpolate(path, smooth(expansion, 0f, .8f))
    }

    fun geometry(source: Rect, target: Rect, progress: Float, corner: Float, foldFraction: Float = .5f): MenuMorphGeometry {
        val ms=progress.coerceIn(0f,1f)*OpenMillis
        if (ms == 0f) return MenuMorphGeometry(source,source.height/2f)
        if (ms == OpenMillis.toFloat()) return MenuMorphGeometry(target,min(corner,target.minSide/2f))
        val compactWidth=source.width*(.8f-.25f*(source.width/source.height-1f).coerceIn(0f,1f))
        val growth=curve(ms,0f to 0f,17f to 0f,33f to .16f,50f to .33f,100f to .73f,
            150f to .94f,200f to 1.012f,250f to 1.01f,350f to 1.002f,500f to 1f)
        val width=if(ms<17f) curve(ms,0f to source.width,17f to compactWidth)
            else compactWidth+(target.width-compactWidth)*growth
        val height=curve(ms,0f to source.height,17f to source.height*1.03f,
            33f to mix(source.height,target.height,.18f),50f to mix(source.height,target.height,.35f),
            100f to target.height*.74f,150f to target.height*.93f,200f to target.height*1.03f,
            250f to target.height*1.02f,350f to target.height*1.003f,500f to target.height)
        val foldX=source.left+source.width*foldFraction
        val cx=curve(ms,0f to source.center.x,17f to foldX,50f to mix(foldX,target.center.x,.35f),
            100f to mix(foldX,target.center.x,.73f),150f to mix(foldX,target.center.x,.94f),
            200f to target.center.x,500f to target.center.x)
        val top=target.top+source.height*curve(ms,0f to 0f,17f to .10f,33f to .65f,
            50f to .75f,100f to .35f,150f to .12f,200f to 0f,500f to 0f)
        val radius=mix(min(width,height)/2f,min(corner,min(width,height)/2f),smooth(ms,100f,240f))
        val skew=(foldFraction-.5f)*2f*source.height*.5f*curve(ms,0f to 0f,17f to 1f,50f to .6f,150f to 0f,500f to 0f)
        return MenuMorphGeometry(Rect(cx-width/2f,top,cx+width/2f,top+height),radius,skew=skew)
    }

    /** 关闭前段直接收拢，不穿过展开末尾的过冲；头部在中段回到源按钮位置。 */
    fun closingGeometry(source: Rect,target: Rect,fraction: Float,corner: Float,foldFraction: Float = .5f): MenuMorphGeometry {
        val ms=fraction.coerceIn(0f,1f)*CloseMillis
        if(ms==0f) return MenuMorphGeometry(target,min(corner,target.minSide/2f))
        if(ms==CloseMillis.toFloat()) return MenuMorphGeometry(source,source.height/2f)
        val width=curve(ms,0f to target.width,33f to target.width*.88f,67f to target.width*.70f,
            100f to target.width*.53f,133f to target.width*.37f,167f to source.height*1.75f,
            200f to source.height*1.2f,233f to source.height,270f to source.width,300f to source.width)
        val height=curve(ms,0f to target.height,33f to target.height*.83f,67f to target.height*.65f,
            100f to mix(source.height,target.height,.36f),133f to mix(source.height,target.height,.28f),
            167f to mix(source.height,target.height,.18f),200f to mix(source.height,target.height,.09f),233f to source.height*1.06f,
            270f to source.height,300f to source.height)
        val foldX=source.left+source.width*foldFraction
        val move=curve(ms,0f to 0f,33f to .23f,67f to .45f,100f to .7f,133f to .94f,167f to 1f,300f to 1f)
        val cx=mix(target.center.x,foldX,move)+(source.center.x-foldX)*smooth(ms,200f,270f)
        val top=source.top+source.height*curve(ms,0f to ((target.top-source.top)/source.height),
            33f to 1f,67f to 1.1f,100f to .7f,133f to 0f,300f to 0f)
        val radius=mix(min(corner,min(width,height)/2f),min(width,height)/2f,smooth(ms,0f,90f))
        val head=smooth(ms,85f,133f)*(1f-smooth(ms,233f,270f))
        return MenuMorphGeometry(Rect(cx-width/2f,top,cx+width/2f,top+height),radius,
            topCorner=mix(radius,source.height/2f,smooth(ms,35f,133f)),
            skew=(source.center.x-cx)*(head+.25f*(1f-head)*smooth(ms,20f,65f)),
            neck=.28f*smooth(ms,90f,150f)*(1f-smooth(ms,200f,270f)),
            headScale=mix(1f,max(1f,source.width/width),head), headDepth=source.height/height)
    }

    fun targetBounds(source: Rect,width: Float,height: Float,screenWidth: Float,screenHeight: Float,
                     safeTop: Float,safeBottom: Float,margin: Float): Rect {
        val w=width.coerceAtMost((screenWidth-margin*2).coerceAtLeast(1f))
        val h=height.coerceAtMost((screenHeight-safeTop-safeBottom-margin).coerceAtLeast(1f))
        val left=(source.right-w).coerceIn(margin,(screenWidth-margin-w).coerceAtLeast(margin))
        val top=source.top.coerceIn(safeTop,(screenHeight-safeBottom-margin-h).coerceAtLeast(safeTop))
        return Rect(left,top,left+w,top+h)
    }
}

private val Rect.minSide get() = min(width,height)
private fun mix(a: Float,b: Float,t: Float) = a+(b-a)*t

/** 单一连续表面：上下圆角、偏斜和颈部作用于同一轮廓，不并置两个胶囊。 */
internal data class MenuMorphGeometry(
    val body: Rect,val bodyCorner: Float,
    val topCorner: Float = bodyCorner,val skew: Float = 0f,
    val neck: Float = 0f,val headScale: Float = 1f,val headDepth: Float = 0f
) {
    fun translated(offset: Offset) = copy(body=body.translate(offset))
    fun interpolate(other: MenuMorphGeometry, t: Float) = MenuMorphGeometry(
        Rect(mix(body.left, other.body.left, t), mix(body.top, other.body.top, t),
            mix(body.right, other.body.right, t), mix(body.bottom, other.body.bottom, t)),
        mix(bodyCorner, other.bodyCorner, t), mix(topCorner, other.topCorner, t),
        mix(skew, other.skew, t), mix(neck, other.neck, t),
        mix(headScale, other.headScale, t), mix(headDepth, other.headDepth, t)
    )
    private fun headWeight(v: Float) = 1f-ActionMenuMotion.smooth(v,headDepth*.55f,min(headDepth+.25f,1f))
    private fun widthScale(v: Float) = 1f+(headScale-1f)*headWeight(v)-
        neck*exp(-((v-min(headDepth+.12f,.8f))/.12f).pow(2))
    private fun shift(v: Float) = skew*if(headDepth>0f) headWeight(v) else (1f-v).pow(2)
    fun distance(x: Float,y: Float): Float {
        val v=((y-body.top)/body.height).coerceIn(0f,1f)
        val localX=(x-body.center.x-shift(v))/widthScale(v)
        val r=if(y<body.center.y) topCorner else bodyCorner
        val qx=abs(localX)-body.width/2f+r; val qy=abs(y-body.center.y)-body.height/2f+r
        return hypot(max(qx,0f),max(qy,0f))+min(max(qx,qy),0f)-r
    }
    fun outline(): Path = Path().apply {
        // 逐行解析同一距离函数的左右边界，避免径向搜索漏掉颈部轮廓。
        for(side in listOf(-1f,1f)) for(j in 0..96) {
            val i=if(side<0f) j else 96-j
            val v=(1f-cos(PI.toFloat()*i/96f))/2f
            val y=body.top+body.height*v
            val r=if(v<.5f) topCorner else bodyCorner
            val edge=max(r-min(y-body.top,body.bottom-y),0f)
            val half=body.width/2f-r+sqrt(max(r*r-edge*edge,0f))
            val x=body.center.x+shift(v)+side*half*widthScale(v)
            if(side<0f && j==0) moveTo(x,y) else lineTo(x,y)
        }
        close()
    }
}

internal const val MenuMorphSdf = """
uniform float4 body;
uniform float2 corners;
uniform float3 warp;
uniform float headDepth;
float2 unwarp(float2 p) {
    float v=clamp((p.y-body.y)/(body.w-body.y),0.0,1.0);
    float head=1.0-smoothstep(headDepth*0.55,min(headDepth+0.25,1.0),v);
    float s=1.0+(warp.z-1.0)*head-warp.y*exp(-pow((v-min(headDepth+0.12,0.8))/0.12,2.0));
    float shift=warp.x*(headDepth>0.0?head:pow(1.0-v,2.0));
    return float2((body.x+body.z)*0.5+(p.x-(body.x+body.z)*0.5-shift)/s,p.y);
}
float distanceField(float2 p) {
    float2 local=unwarp(p);
    float radius=p.y<(body.y+body.w)*0.5?corners.x:corners.y;
    float2 q=abs(local-(body.xy+body.zw)*0.5)-(body.zw-body.xy)*0.5+radius;
    return length(max(q,0.0))+min(max(q.x,q.y),0.0)-radius;
}
float2 normalAt(float2 p) {
    float2 n=float2(distanceField(p+float2(0.5,0.0))-distanceField(p-float2(0.5,0.0)),
                    distanceField(p+float2(0.0,0.5))-distanceField(p-float2(0.0,0.5)));
    return n/max(length(n),0.001);
}
"""

internal const val MenuMorphLens = """
uniform shader content;
uniform float2 lens;
half4 main(float2 p) {
    float depth=max(-distanceField(p),0.0);
    float edge=clamp(1.0-depth/max(lens.x,0.001),0.0,1.0);
    float displacement=(1.0-sqrt(max(1.0-edge*edge,0.0)))*lens.y;
    return content.eval(p-displacement*normalAt(p));
}
"""

internal const val MenuMorphContentLens = """
uniform shader content;
uniform float2 lens;
uniform float4 destination;
half4 main(float2 p) {
    float depth=max(-distanceField(p),0.0);
    float edge=clamp(1.0-depth/max(lens.x,0.001),0.0,1.0);
    float displacement=(1.0-sqrt(max(1.0-edge*edge,0.0)))*lens.y;
    float2 local=unwarp(p-displacement*normalAt(p));
    float2 uv=(local-body.xy)/(body.zw-body.xy);
    // 内容随表面流入，但不把全部行强行等比塞进早期的小滴。
    float2 mapped=destination.xy+uv*(destination.zw-destination.xy);
    return content.eval(mix(local,mapped,0.45));
}
"""

internal const val MenuMorphReflection = """
uniform float headroom;
half4 main(float2 p) {
    half weight=half(pow(abs(normalAt(p).y),8.0));
    return half4(fromLinearSrgb(half3(headroom))*weight,weight);
}
"""
