uniform mat4 u_MVPMatrix;                      // 一个表示组合model、view、projection矩阵的常量
uniform mat4 u_MVMatrix;                       // 一个表示组合model、view矩阵的常量

attribute vec4 a_Position;                     // 我们将要传入的每个顶点的位置信息
attribute vec4 a_Color;                        // 我们将要传入的每个顶点的颜色信息
attribute vec3 a_Normal;                       // 我们将要传入的每个顶点的法线信息
attribute vec2 a_TexCoordinate;                // 我们将要传入的每个顶点的纹理坐标信息

varying vec3 v_Position;                      
varying vec4 v_Color;                         
varying vec3 v_Normal;                        
varying vec2 v_TexCoordinate;                  // 这将会传入到片段着色器

// 顶点着色器入口点
void main()
{
   // 传入纹理坐标
   v_TexCoordinate = a_TexCoordinate;         
   // 将顶点位置转换成眼睛空间的位置
   v_Position = vec3(u_MVMatrix * a_Position);
   // 传入颜色
   v_Color = a_Color;
   // 将法线的方向转换在眼睛空间
   v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));
   // gl_Position是一个特殊的变量用来存储最终的位置
   // 将顶点乘以矩阵得到标准化屏幕坐标的最终点
   gl_Position = u_MVPMatrix * a_Position;
}
