package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.*

/** 09-05 / 09-11 连续帧约束下的近似轨迹；不是 Apple 内部动画参数。 */
internal enum class MenuSourceSurface { Glass, Icon }

internal object ActionMenuMotion {
    const val OpenMillis = 500
    fun dismissMillis(surface: MenuSourceSurface) = if (surface == MenuSourceSurface.Glass) 480 else 320
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

    fun geometry(source: Rect, target: Rect, progress: Float, corner: Float, foldFraction: Float = .5f): MenuMorphGeometry {
        val ms=progress.coerceIn(0f,1f)*OpenMillis
        if (ms == 0f) return MenuMorphGeometry(source,source.height/2f)
        if (ms == OpenMillis.toFloat()) return MenuMorphGeometry(target,min(corner,target.minSide/2f))
        val compactWidth=source.width*(.8f-.25f*(source.width/source.height-1f).coerceIn(0f,1f))
        val growth=curve(ms,0f to 0f,17f to 0f,33f to .16f,50f to .33f,100f to .73f,
            150f to .98f,200f to 1f,500f to 1f)
        val width=if(ms<17f) curve(ms,0f to source.width,17f to compactWidth)
            else curve(ms,17f to compactWidth,33f to mix(compactWidth,target.width,.16f),
                50f to mix(compactWidth,target.width,.33f),100f to mix(compactWidth,target.width,.73f),
                150f to target.width*.98f,200f to target.width*1.026f,270f to target.width*1.014f,
                350f to target.width*1.002f,450f to target.width,500f to target.width)
        val foldX=source.left+source.width*foldFraction
        // 横向先回正，上边缘稍后转向；不围绕固定中心同时缩放整张菜单。
        val right=curve(ms,0f to source.right,17f to foldX+compactWidth/2f,
            100f to mix(foldX+compactWidth/2f,target.right,.92f),150f to target.right,500f to target.right)
        val lift=min(target.height*.009f,source.height*.22f)
        val top=mix(source.top,target.top,growth)+source.height*curve(ms,0f to 0f,17f to .10f,33f to .65f,
            50f to .75f,100f to .35f,150f to .12f,215f to 0f,500f to 0f)-
            lift*curve(ms,0f to 0f,215f to 0f,270f to 1f,350f to .3f,450f to 0f,500f to 0f)
        val bottom=curve(ms,0f to source.bottom,17f to source.bottom+source.height*.13f,
            50f to mix(source.bottom,target.bottom,.38f),100f to mix(source.bottom,target.bottom,.78f),
            150f to target.bottom,180f to target.bottom+target.height*.045f,
            270f to target.bottom+target.height*.01f,350f to target.bottom,500f to target.bottom)
        val height=(bottom-top).coerceAtLeast(source.height*.5f)
        val radius=mix(min(width,height)/2f,min(corner,min(width,height)/2f),smooth(ms,100f,240f))
        val skew=(foldFraction-.5f)*2f*source.height*.5f*curve(ms,0f to 0f,17f to 1f,50f to .6f,150f to 0f,500f to 0f)
        return MenuMorphGeometry(Rect(right-width,top,right,top+height),radius,skew=skew)
    }

    /** 以实际轮廓开始，玻璃/裸图标仅决定终态和收尾时钟，不依赖菜单项数量或业务页。 */
    fun dismissGeometry(start: MenuMorphGeometry, source: Rect, target: Rect, progress: Float,
                        surface: MenuSourceSurface, upward: Boolean = false): MenuMorphGeometry {
        val p = progress.coerceIn(0f, 1f)
        if (p == 0f) return start
        if (upward) return dismissGeometry(start.mirrored(), mirror(source), mirror(target), p, surface).mirrored()
        val end=MenuMorphGeometry(source,source.minSide/2f)
        if (p == 1f) return end
        val ms=p*dismissMillis(surface)
        val glass=surface==MenuSourceSurface.Glass
        // 主体约 260ms 收完；顶部胶囊继续用约 220ms 消化向上的越位。
        val finish=if(glass) 480f else 320f
        val sw=source.width;val sh=source.height
        val width=curve(ms,0f to start.body.width,33f to mix(sw,start.body.width,.88f),
            67f to mix(sw,start.body.width,.65f),100f to mix(sw,start.body.width,.44f),
            150f to max(sh, start.body.width*.26f),200f to max(sh,sw*.66f),
            260f to sw*.95f,finish to sw)
        val height=curve(ms,0f to start.body.height,33f to mix(sh,start.body.height,.83f),
            67f to mix(sh,start.body.height,.60f),100f to mix(sh,start.body.height,.39f),
            150f to mix(sh,start.body.height,.22f),200f to mix(sh,start.body.height,.08f),
            260f to sh*.97f,finish to sh)
        val move=curve(ms,0f to 0f,33f to .18f,67f to .4f,100f to .65f,150f to .92f,200f to 1f,finish to 1f)
        val cx=mix(start.body.center.x,source.center.x,move)
        val top=mix(start.body.top,source.top,smooth(ms,0f,140f))+sh*curve(ms,
            0f to 0f,33f to .8f,67f to 1f,100f to .6f,150f to -.06f,
            210f to (if(glass) -.13f else -.08f),260f to (if(glass) -.12f else -.04f),finish to 0f)
        val radius=mix(min(start.bodyCorner,min(width,height)/2f),min(width,height)/2f,smooth(ms,0f,90f))
        val head=smooth(ms,85f,150f)*(1f-smooth(ms,220f,260f))
        val carry=1f-smooth(ms,0f,90f)
        val path=MenuMorphGeometry(Rect(cx-width/2f,top,cx+width/2f,top+height),radius,
            topCorner=min(min(width,height)/2f,mix(radius,sh/2f,smooth(ms,35f,150f)))+
                (start.topCorner-start.bodyCorner)*carry,
            skew=(source.center.x-cx)*head+start.skew*carry,
            neck=.30f*smooth(ms,80f,155f)*(1f-smooth(ms,200f,260f))+start.neck*carry,
            headScale=mix(1f,max(1f,sw/width),head)+(start.headScale-1f)*carry,
            headDepth=mix(start.headDepth,(sh/height).coerceAtMost(1f),smooth(ms,0f,85f)))
        // 展开初段被打断时不制造一张完整菜单或额外大液滴。
        val expansion=((start.body.height-sh)/(target.height-sh).coerceAtLeast(1f)).coerceIn(0f,1f)
        return start.interpolate(end,smooth(p,0f,1f)).interpolate(path,smooth(expansion,0f,.8f))
    }

    fun dismissalAlpha(progress: Float, surface: MenuSourceSurface) =
        if(surface==MenuSourceSurface.Glass) 1f else 1f-smooth(progress,.72f,1f)

    fun closingSourceAlpha(progress: Float, surface: MenuSourceSurface) =
        smooth(progress*dismissMillis(surface),110f,190f)

    fun closingMaterialProgress(progress: Float, surface: MenuSourceSurface) =
        1f-smooth(progress*dismissMillis(surface),0f,140f)

    fun openingGeometry(source: Rect, target: Rect, progress: Float, corner: Float, fold: Float, upward: Boolean): MenuMorphGeometry {
        if (!upward) return geometry(source, target, progress, corner, fold)
        val g = geometry(mirror(source), mirror(target), progress, corner, fold)
        return g.mirrored()
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
private fun mirror(rect: Rect) = Rect(rect.left,-rect.bottom,rect.right,-rect.top)

/** 单一连续表面：上下圆角、偏斜和颈部作用于同一轮廓，不并置两个胶囊。 */
internal data class MenuMorphGeometry(
    val body: Rect,val bodyCorner: Float,
    val topCorner: Float = bodyCorner,val skew: Float = 0f,
    val neck: Float = 0f,val headScale: Float = 1f,val headDepth: Float = 0f,
    val flipped: Boolean = false
) {
    fun mirrored() = copy(body=mirror(body),flipped=!flipped)
    fun translated(offset: Offset) = copy(body=body.translate(offset))
    fun interpolate(other: MenuMorphGeometry, t: Float) = MenuMorphGeometry(
        Rect(mix(body.left, other.body.left, t), mix(body.top, other.body.top, t),
            mix(body.right, other.body.right, t), mix(body.bottom, other.body.bottom, t)),
        mix(bodyCorner, other.bodyCorner, t), mix(topCorner, other.topCorner, t),
        mix(skew, other.skew, t), mix(neck, other.neck, t),
        mix(headScale, other.headScale, t), mix(headDepth, other.headDepth, t), flipped
    )
    private fun headWeight(v: Float) = 1f-ActionMenuMotion.smooth(v,headDepth*.55f,min(headDepth+.25f,1f))
    private fun widthScale(v: Float) = 1f+(headScale-1f)*headWeight(v)-
        neck*exp(-((v-min(headDepth+.12f,.8f))/.12f).pow(2))
    private fun shift(v: Float) = skew*if(headDepth>0f) headWeight(v) else (1f-v).pow(2)
    fun distance(x: Float,screenY: Float): Float {
        val y=if(flipped) body.top+body.bottom-screenY else screenY
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
            val screenY=if(flipped) body.top+body.bottom-y else y
            if(side<0f && j==0) moveTo(x,screenY) else lineTo(x,screenY)
        }
        close()
    }
}

internal const val MenuMorphSdf = """
uniform float4 body;
uniform float2 corners;
uniform float3 warp;
uniform float headDepth;
uniform float flipped;
float2 unwarp(float2 p) {
    if(flipped>0.5) p.y=body.y+body.w-p.y;
    float v=clamp((p.y-body.y)/(body.w-body.y),0.0,1.0);
    float head=1.0-smoothstep(headDepth*0.55,min(headDepth+0.25,1.0),v);
    float s=1.0+(warp.z-1.0)*head-warp.y*exp(-pow((v-min(headDepth+0.12,0.8))/0.12,2.0));
    float shift=warp.x*(headDepth>0.0?head:pow(1.0-v,2.0));
    return float2((body.x+body.z)*0.5+(p.x-(body.x+body.z)*0.5-shift)/s,p.y);
}
float distanceField(float2 p) {
    float2 local=unwarp(p);
    float radius=local.y<(body.y+body.w)*0.5?corners.x:corners.y;
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
    // 内部零位移时不计算四次距离采样的法线，采样坐标保持不变。
    if (displacement == 0.0) return content.eval(p);
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
    float2 refracted=p;
    if (displacement != 0.0) refracted-=displacement*normalAt(p);
    float2 local=unwarp(refracted);
    if(flipped>0.5) local.y=body.y+body.w-local.y;
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
