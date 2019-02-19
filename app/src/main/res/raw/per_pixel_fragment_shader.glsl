precision mediump float; //我们将默认精度设置为中等，我们不需要片段着色器中的高精度
uniform sampler2D u_Texture;  // 传入纹理
uniform vec3 u_LightPos; // 光源在眼睛空间的位置
varying vec3 v_Position; // 插入的位置
varying vec4 v_Color; // 插入的位置颜色
varying vec3 v_Normal; // 插入的位置法线
varying vec2 v_TexCoordinate; // 插入的纹理坐标
void main()  // 片段着色器入口
{
   // 将用于哀减
   float distance = length(u_LightPos - v_Position);
   // 获取从光源到顶点方向的光线向量
   vec3 lightVector = normalize(u_LightPos - v_Position);
   // 计算光线矢量和顶点法线的点积，如果法线和光线矢量指向相同的方向，那么它将获得最大的照明
   float diffuse = max(dot(v_Normal, lightVector), 0.1);
   // 根据距离哀减光线
   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));
   // 添加环境照明
   diffuse = diffuse + 0.3;
   // 颜色乘以亮度哀减和纹理值得到最终的颜色
   gl_FragColor = v_Color * diffuse * texture2D(u_Texture, v_TexCoordinate);
}
