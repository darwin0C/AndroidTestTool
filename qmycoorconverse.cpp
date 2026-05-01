#include "qmycoorconverse.h"
#include <QDebug>
#include <QDir>
#include <QTimeZone>
#include <QtMath>

QMyCoorConverse::QMyCoorConverse(QObject *parent)
    : QObject(parent)
{

}

QMyCoorConverse::~QMyCoorConverse()
{

}
//======================================================================
//BED转换成XYH
//B E D均为弧度
void QMyCoorConverse::changeBEDToXYH(double B,double E,ushort D,double &x,double &y,double &h)
{
    double cosE = qCos(E);
    x = D * qCos(B) * cosE;
    y = D * qSin(B) * cosE;
    h = D * qSin(E);
}
//=============================================================================
/*---------------------------------------------------------------------------
功能：		用坐标从BLH计算带号. 
传入参数：
B	  - 大地坐标系纬度值(单位度)
L	  - 大地坐标系经度值(单位度)
H	  - 大地坐标系高程值(单位米)
Gz    - 经度（1-60） 
ASII  - 纬度（C-X） 
返回值：没有返回值，需要的返回值通过B，L，H指针变量返回
---------------------------------------------------------------------------*/
void QMyCoorConverse::BLH_TO_GZ( double L,double B, unsigned short &Gz,unsigned char &ASII)
{
    unsigned char tempASII=0;
    //int Gz,ASII;
    if((L>=-180)&&(L<+180))
    {
        if (L>0)
        {
            Gz=L/6+31;
        }else
        {
            Gz=(L+180)/6+1;
        }
    }
    if((B>= -80)&&(B<72))
    {
        tempASII = (B + 80)/8 + 67;
        if(tempASII < 73)
        {
            ASII = tempASII;
        }else if(tempASII <78)
        {
            ASII = tempASII + 1;
        }else
        {
            ASII = tempASII + 2;
        }
    }else if((B>=72)&&(B<=84))
    {
        ASII = 88;
    }
}
//===================================================
//度分秒转度
double QMyCoorConverse::dDuFenMiao_dDu( double dData )
{
    int iDu = static_cast<int>(dData);
    double temp = (dData - iDu) * 100.0;
    int iFen = static_cast<int>(temp);
    double dMiao = (temp - iFen) * 100.0;

    return iDu + iFen / 60.0 + dMiao / 3600.0;
}
//==================================================
//20180814	 utmXY2BL
//弧度转经纬度，经纬度表示方式：度.分分秒秒秒秒 如19.235342表示19度23分53.42秒
//西半球的纬度为“-”，南半球的经度为“-”
double QMyCoorConverse::RadtoDfm(double Rad)   
{   
    double Degree, Miniute;
    double Second;
    int Sign;
    double Dfm;
    if(Rad >= 0)
        Sign = 1;
    else
        Sign = -1;
    Rad = fabs(Rad * 180.0 / MYPI);
    Degree = floor(Rad);
    Miniute = floor(fmod(Rad * 60.0, 60.0));
    Second = fmod(Rad * 3600.0, 60.0);
    Dfm = Sign * (Degree + Miniute / 100.0 + Second / 10000.0);
    return Dfm;
}  

/****************************************************************************
** GossXYtoLaLo
** 输入：		   a                椭球体长半轴
**                 b                椭球体短半轴
**                 FN               纬度起始点，北半球为0，南半球为10000000.0m
**                 x        		经过Goss投影后的纬度方向的坐标
**                 y        		经过Goss投影后的经度方向的坐标      
**                 B                经过Goss投影之前的纬度（度分秒）
**                 L                经过Goss投影之前的经度（度分秒）
** 功能描述：Goss投影反转
***************************************************************************/	
void QMyCoorConverse::GossXYtoLaLo(double a,double b,double FN,double x,double y,double &B,double &L)
{
    double f,e,e1;

    f = (a-b)/b;
    e = sqrt(1-(b/a)*(b/a));
    e1 = sqrt((a/b)*(a/b)-1);

    double Nf,Rf,Bf,Fi,e11,Mf,Fn,k0,Tf,Cf,D,FE;
    int rib = y/1000000;
    FE = 500000 + 1000000*rib;
    double l0 = (rib *6 -3)*MYPI/180;
    if (rib > 30)
        l0 = l0-2*MYPI;
    //	Fn = 0.0;
    k0 = 1.0;
    Mf = (x - FN)/k0;
    Fi = Mf/(a*(1-e*e/4.0 - 3*e*e*e*e/64.0 - 5*e*e*e*e*e*e/256.0));
    e11 = (a - b)/(a + b);
    Bf = Fi + (3*e11/2.0 - 27*e11*e11*e11/32.0)*sin(2*Fi) + (21*e11*e11/16.0 - 55*e11*e11*e11*e11/32.0) * sin(4*Fi)
            + (151 * e11 * e11 *e11/96.0) * sin(6*Fi);
    Rf = a*(1-e*e)/pow((1-e*e*sin(Bf)*sin(Bf)),3/2.0);
    Nf = (a*a/b)/sqrt(1+e1*e1*cos(Bf)*cos(Bf));
    //	Nf = a/sqrt(1-e*e*sin(Bf)*sin(Bf));
    Tf = tan(Bf)*tan(Bf);
    Cf = e1*e1*cos(Bf)*cos(Bf);
    D = (y-FE)/(k0*Nf);

    B = Bf - Nf*tan(Bf)/Rf*(D*D/2.0 - (5+3*Tf+Cf-9*Tf*Cf)*D*D*D*D/24.0 + (61+90*Tf+ 45*Tf*Tf)*D*D*D*D*D*D/720.0);
    L = l0+1/cos(Bf)*(D-(1+2*Tf+Cf)*D*D*D/6.0 + (5+28*Tf +6*Cf+8*Tf*Cf+24*Tf*Tf)*D*D*D*D*D/120.0);

    B = RadtoDfm(B) ;
    L = RadtoDfm(L);
}

/****************************************************************************
** UTMXYtoLaLo
** 输入：		   a                椭球体长半轴
**                 b                椭球体短半轴
**                 FN               纬度起始点，北半球为0，南半球为10000000.0m
**                 x        		经过UTM投影后的纬度方向的坐标
**                 y        		经过UTM投影后的经度方向的坐标      
**                 B                经过UTM投影之前的纬度（度分秒）
**                 L                经过UTM投影之前的经度（度分秒）
** 功能描述：UTM投影反转

***************************************************************************/
void QMyCoorConverse::UTMXYtoLaLo(double a,double b,double fn,double x,double y,double &lat,double &lon)
//    ** a 椭球体长半轴
//    ** b 椭球体短半轴
//    ** y 经过UTM投影后的经度方向的坐标，也就是UTMEasting
//    ** x 经过UTM投影后的纬度方向的坐标，也就是UTMNorthing
//    ** FN  纬度起始点，北半球为0，南半球为10000000.0m
//    ** lon0 中央经度线(弧度)
//    ** lat 纬度（度分秒）
//    ** lon 经度（度分秒）
//    ** 功能描述：UTM坐标转换为经纬度坐标
{
    /*	if (x>10000000)
    {
    x -=10000000;
    }*/
    /*#if defined R122//2016.12.23负值表示南半球
    if(x<0)
    {
    x = (-1) * x;
    }
    #else*/
    if (fn == 10000000 )//2015.7.4用Fn判断是否为南半球
    {
        x = 10000000 - x;
    }
    unsigned char flag = 0;
    if(x<0)//2017.1.10用x为负判断是否为南半球
    {
        x = (-1) * x;
        flag = 1;
    }

    //#endif
    double lon0 ;
    int rib = y/1000000;
    int temprib = rib-30;
    lon0 = (temprib *6 -3)*MYPI/180;

    y = 500000.0 + rib*1000000 - y;
    double k0 = 0.9996;
    double e = sqrt(1-(b*b)/(a*a));
    // calculate the meridional arc
    double M = x/k0;
    // calculate footprint latitude
    double mu = M/(a*(1 - pow(e,2)/4.0 - 3*pow(e,4)/64.0 - 5*pow(e,6)/256.0));
    double e1 = (1 - sqrt(1 - pow(e,2)))/(1 + sqrt(1 - pow(e,2)));
    double J1 = (3*e1/2.0 - 27*pow(e1,3)/32.0);
    double J2 = (21*e1*e1)/16.0 - 55*pow(e1,4)/32.0;
    double J3 = (151*pow(e1,3)/96.0);
    double J4 = (1097*pow(e1,4)/512.0);
    double fp = mu + J1*sin(2*mu) + J2*sin(4*mu) + J3*sin(6*mu) + J4*sin(8*mu);

    //Calculate Latitude and Longitude

    double e2 = pow(e,2)/(1-pow(e,2));
    double C1 = e2*cos(fp)*cos(fp);
    double T1 = tan(fp)*tan(fp);
    double R1 = a*(1-e*e)/pow(1-(e*sin(fp))*(e*sin(fp)),3.0/2);// # This is the same as rho in the forward conversion formulas above, but calculated for fp instead of lat.;
    double N1 = a/sqrt(1-(e*sin(fp))*(e*sin(fp)));//   # This is the same as nu in the forward conversion formulas above, but calculated for fp instead of lat.
    double D = y/(N1*k0);

    double Q1 = N1*tan(fp)/R1;
    double Q2 = (D*D/2.0);
    double Q3 = (5 + 3*T1 + 10*C1 - 4*C1*C1 -9*e2)*pow(D,4)/24.0;
    double Q4 = (61 + 90*T1 + 298*C1 +45*T1*T1  - 3*C1*C1 -252*e2)*pow(D,6)/720.0;
    lat = RadtoDfm(fp - Q1*(Q2 - Q3 + Q4));
    /*#if defined R122//2016.12.23负值表示南半球
    if(x<0)
    {
    lat = (-1) * lat;
    }
    #else*/
    if (fn == 10000000)
    {
        lat *= -1;
    }
    if(flag == 1)
    {
        lat = (-1) * lat;
    }

    //#endif

    double Q5 = D;
    double Q6 = (1 + 2*T1 + C1)*pow(D,3)/6.0;
    double Q7 = (5 - 2*C1 + 28*T1 - 3*C1*C1 + 8*e2 + 24*T1*T1)*pow(D,5)/120.0;
    lon = RadtoDfm(lon0 - (Q5 - Q6 + Q7)/cos(fp));
}


//功能：根据不同的椭球，不同的投影方式，投影坐标转经纬度
//参数：Northing:转换的北向坐标；Easting：转换的东向坐标；lh:测出的高程单位米
//oveltype:椭球类型0：WGS84，1：BJ-54，2：CLARKE 1880；projtype:投影方式0:UTM,1:Gauss
//lon:经度(度.分分秒秒秒秒)；lat:纬度(度.分分秒秒秒秒)；zh:转换后的高程单位米
void QMyCoorConverse::XYtoLonLat(double Northing,double Easting,double lh,unsigned char oveltype,unsigned char projtype,bool hemisphere,
                                 double &lon,double &lat,double &zh)
{
    double m_a = 0;//长轴
    double m_b = 0;//短轴
    double m_L = 0;//经度“度.度度度度”格式
    double m_B = 0;//纬度“度.度度度度”格式
    zh = lh;
    switch(oveltype)//椭球类型
    {
    case 0://WGS84
    {
        m_a = 6378137.0;
        m_b = 6356752.3142452;
        break;
    }
    case 1://BJ-54
        m_a = 6378245.0;
        m_b = 6356863.01880;
        break;
    case 2://CLARKE 1880
    {
        m_a = 6378249.145;
        m_b = 6356514.87000;
    }
    default:
        break;
    }
    switch (projtype)//投影方式：0：UTN投影，1：
    {
    case 0://UTN投影
    {
        double fn = 0;
        // 			if (Northing>NORTHSOUTH)//2017.1.10南半球   NORTHSOUTH:6000000
        // 			{
        // 				fn = 10000000;
        // 			}
        if (hemisphere)
            fn = 10000000;
        UTMXYtoLaLo(m_a,m_b,fn,Northing,Easting,lat,lon);
        break;
    }
    case 1://高斯投影
    {
        GossXYtoLaLo(m_a,m_b,0,Northing,Easting,lat,lon);
        break;
    }
    default:
        break;
    }
}

//本地大地坐标与UTM投影坐标函数
/****************************************************************************
** UTMLaLotoXy
**	double a = 6378137.0,b = 6356752.3142452;
** 输入：		   a                椭球体长半轴
**                 b                椭球体短半轴
**                 Lat              纬度，单位：度(以度为单位的浮点数据)
**                 Long             经度，单位：度
**                 FN               纬度起始点，北半球为0，南半球为10000000.0m,本函数调用时写0
**                 UTMNorthing		经过UTM投影后的纬度方向的坐标
**                 UTMEasting		经过UTM投影后的经度方向的坐标      
** 功能描述：UTM投影正转
** 20150429该函数的使用地方. 1:需要在获取GPS大地坐标的地方使用
**                           2:在手输入的地方输入
** 调用示例: void UTMLaLotoXy(6378137.0, 6356752.3142452, this->m_SEV_Device[dnum-1].latitude, this->m_SEV_Device[dnum-1].longitude, 
0,  this->m_SEV_Device[dnum-1].LocalX_N,  this->m_SEV_Device[dnum-1].LocalY_E)
***************************************************************************/
// _declspec(dllexport) void UTMLaLotoXy(double a,double b,double Lat, double Long, 
// 								  double FN, double &UTMNorthing, double &UTMEasting)


// void QMyCoorConverse::UTMLaLotoXy(double a,double b,double Lat, double Long, 
// 								   double FN, double &UTMNorthing, double &UTMEasting)
void QMyCoorConverse::UTMLaLotoXy( double Long, double Lat,double FN, double &UTMNorthing, double &UTMEasting)
{
    double a=6378137.0;
    double b=6356752.3142452;
    if(Lat>=0)
        FN = 0;
    else
        FN = 10000000;
    Lat = fabs(Lat);
    //e表示WGS84第一偏心率,eSquare表示e的平方,
    double f = (a-b)/a;
    double eSquare =(2*f-f*f) ;
    double k0 = 0.9996;
    double e2Square;
    double V, T, C, A, M;

    int Rib = (int)(abs(Long)/6.0);
    Rib +=1;
    double LongOrigin = Rib*6-3;

    if (Long<0)
    {
        Rib = (360+Long)/6 +1 - 30;
        LongOrigin = -1*LongOrigin;
    }
    else
        Rib = Rib + 30;

    double LatRad = Lat*MYPI/180;
    double LongRad = Long*MYPI/180;
    double LongOriginRad;
    LongOriginRad = LongOrigin * MYPI/180;
    e2Square = (eSquare)/(1-eSquare);

    V = a/sqrt(1-eSquare*sin(LatRad)*sin(LatRad));
    T = tan(LatRad)*tan(LatRad);
    C = e2Square*cos(LatRad)*cos(LatRad);
    A = cos(LatRad)*(LongRad-LongOriginRad);
    M = a*((1-eSquare/4-3*eSquare*eSquare/64-5*eSquare*eSquare*eSquare/256)*LatRad-(3*eSquare/8+3*eSquare*eSquare/32+45*eSquare*eSquare*eSquare/1024)*sin(2*LatRad)+(15*eSquare*eSquare/256+45*eSquare*eSquare*eSquare/1024)*sin(4*LatRad) -(35*eSquare*eSquare*eSquare/3072)*sin(6*LatRad));

    UTMEasting = (double)(k0*V*(A+(1-T+C)*A*A*A/6 + (5-18*T+T*T+72*C-58*e2Square)*A*A*A*A*A/120)+ 500000.0 + Rib*1000000);
    UTMNorthing = (double)(k0*(M+V*tan(LatRad)*(A*A/2+(5-T+9*C+4*C*C)*A*A*A*A/24+ (61-58*T+T*T+600*C-330*e2Square)*A*A*A*A*A*A/720)));
    //南半球纬度起点为10000000.0m
    UTMNorthing=UTMNorthing+FN;
}




//

//==============================================================================
//	功能:
//			设备坐标 --> 大地坐标（目标求解）
//	参数:
//          dBy:		设备瞄线与设备零位的夹角     A
//          dEy:  		设备瞄线与设备回转平面的夹角 E
//          dDy:  		设备测距值                   D
//          dAL: 		设备零位与坐标北方向的夹角     N
//          dST:  		设备瞄线方向的横倾角         R
//          dFI:  		设备瞄线方向的纵倾角         P
//  返回：
//      *dX,*dY,*dH:    指针,返回大地坐标X,Y,H
//  注解：  所有角变量都用弧度
void QMyCoorConverse::CoorConvertDevice2XY(  double dBy,  double dEy,  double dDy,
                                             double dAL,  double dST,  double dFI,
                                             double *dX,  double *dY,  double *dH )
{
    double dXy,  dYy,  dHy;
    double dAL0, dST0, dFI0;

    //  姿态传感器输出数据转换(α/θ/φ) 修正到观瞄仪零位角的寻北、纵摇、横摇
    dAL0 = dAL; dST0 = dST; dFI0 = dFI;

    //  在设备零位坐标系中 β,ε,d --> x,y,h（方位的零位方向为x轴正向，左手系）
    dXy = dDy * cos(dEy) * cos(dBy);
    dYy = dDy * cos(dEy) * sin(dBy);
    dHy = dDy * sin(dEy);

    //  设备坐标 --> 大地坐标 旋转 ( Xy,Yy,Hy --> X,Y,H )
    CoorRoll( &dYy, &dHy, -dST0 );    //横倾
    CoorRoll( &dXy, &dHy,  dFI0 );    //纵倾
    CoorRoll( &dXy, &dYy, -dAL0 );    //北向

    //  输出结果
    *dX = dXy;
    *dY = dYy;
    *dH = dHy;
}
//===========================================================
//  功能:                                                             |y
//			坐标旋转函数                                               | dAngle /Y'
//	注解:                                                         \   |      /
//                                                                 \  |    /
//			从dY轴向dX旋转dAngle                                     \ |  /
//	参数:                                                    -------- |-------X
//			dY:     坐标系纵轴                                        /|\
//          dX:     坐标系横轴                                      /  |  \
//          dAngle: 旋转角（与纵轴夹角）  弧度                      /    |    \
//  返回：  无                                                  /      |      \ X'
//                                                                    |
void QMyCoorConverse::CoorRoll( double *dY,   double *dX,
                                double dAngle )
{
    double x,y,m[3][3];

    m[1][1] =  cos( dAngle );     m[1][2] = -sin( dAngle );
    m[2][1] =  sin( dAngle );     m[2][2] =  cos( dAngle );

    x = *dX; y = *dY;
    *dX = x * m[1][1] + y * m[1][2];
    *dY = x * m[2][1] + y * m[2][2];
}


//===========================================================
//	功能:
//			带有保护处理的 arctg 函数
//	参数:
//			y,x:	angle = tg( y/x )
//	返回:
//			angle ( 弧度值)
double 	QMyCoorConverse::Protect_atan2( double y, double x )
{
    if ( (x==0)&&(y==0) )
        return	0;
    else
        return atan2(y,x);     //换算成0～2π
}

/*有计算机导引源时调用一次*/
//大地坐标系下的极坐标BE ->转换成设备坐标系AE
//void TfrmMain::Ground_aix_to_Guide_vol(long c_x,long c_y,long c_h,double n)
//参数 b e 是目标大地极坐标系下的角度 单位：弧度
//参数d    是距离值
//double n  设备零位的北向  单位弧度
//r p 解算到零位的姿态
//az el 为设备零位的角度    单位弧度
void QMyCoorConverse::Ground_aix_to_Guide_vol(double b,double e,double d,double n,double r,double p,double *az,double *el)
{
    double x=0;
    double y=0;
    double h=0;
    float sinEpsilon = sin(e);
    float cosEpsilon = cos(e);
    float sinBeta    = sin(b);
    float cosBeta    = cos(b);
    x =d * cosEpsilon * cosBeta;
    y = d * cosEpsilon * sinBeta;
    h = d * sinEpsilon;
    double temaz=0;
    double temel=0;
    Ground_aix_XYH_to_Guide_vol(x,y,h,n,r,p,&temaz,&temel);
    *az=temaz;
    *el=temel;
}

//大地坐标系下的直角坐标XYH ->转换成设备坐标系AE

//参数 dx dy dh 是目标大地直角坐标系下的目标坐标与本站的坐标的差值
//double n  设备零位的北向  单位弧度
//r p 解算到零位的姿态
//az el 为设备零位的角度    单位弧度
void QMyCoorConverse::Ground_aix_XYH_to_Guide_vol(  double dx,double dy,double dh,double n,double r,double p,double *az,double *el)
{

    // qDebug()<<dx<<dy<<dh<<n<<r<<p<<"caltem var";
    //idata float T_beta, T_epsilon;
    float x, y, h, xpp, ypp, hpp;
    float ABC[3][3]={0};
    // float sinAlpha, cosAlpha, sinPhi, cosPhi, sinTheta, cosTheta;

    //    unsigned short    Guide_Fw;
    //    unsigned short    Guide_Gd;

    float sinAlpha   = sin(n);
    float cosAlpha   = cos(n);
    float sinPhi     = sin(p);
    float cosPhi     = cos(p);
    float sinTheta   = sin(r);
    float cosTheta   = cos(r);

    //    float sinEpsilon = sin(b);
    //    float cosEpsilon = cos(b);
    //    float sinBeta    = sin(e);
    //    float cosBeta    = cos(e);

    /*
     x = c_x - g_X0;
     y = c_y - g_Y0;
     h = c_h - g_H0;
 */
    /*----------------------------liyu20070530 ,改为相对坐标*/
    x =dx;
    y = dy;
    h = dh;
    // 由直角坐标到极坐标的转换
    ABC[0][0] =  cosAlpha * cosPhi;
    ABC[0][1] = -sinAlpha * cosTheta + cosAlpha * sinPhi * sinTheta;
    ABC[0][2] =  sinAlpha * sinTheta + cosAlpha * sinPhi * cosTheta;

    ABC[1][0] =  sinAlpha * cosPhi;
    ABC[1][1] =  cosAlpha * cosTheta + sinAlpha * sinPhi * sinTheta;
    ABC[1][2] = -cosAlpha * sinTheta + sinAlpha * sinPhi * cosTheta;

    ABC[2][0] = -sinPhi;
    ABC[2][1] =  cosPhi * sinTheta;
    ABC[2][2] =  cosPhi * cosTheta;

    // 由直角坐标到极坐标的转换
    xpp = ABC[0][0]*x + ABC[1][0]* y + ABC[2][0]*h;
    ypp = ABC[0][1]*x + ABC[1][1]* y + ABC[2][1]*h;
    hpp = ABC[0][2]*x + ABC[1][2]* y + ABC[2][2]*h;


    //采用贺丽的变量
    if(xpp>0)			*az = atan(ypp/xpp);
    else if (xpp<0)  *az= atan(ypp/xpp) + MYPI;//*0.5
    else if (ypp>0)		*az = MYPI*0.5;//*0.25
    else  				*az = MYPI*1.5;//-16384; 0.75
    *el 	= atan(hpp/sqrt(x*x+y*y));
    qDebug()<<*az<<az<<*el<<el;
}

int QMyCoorConverse::coorConvert(Position_Unique &myPosition,double long_e,double lati_n,double h,unsigned char coorflag, unsigned char hemisphereflag)
{
    double tfn=0;
    double teast=0;
    double tnorth=0;
    double tlatitude=0;
    double tlongitude=0;
    unsigned short teastid=0;
    unsigned char  tnortthid=0;
    double tlat=0;
    double tlon=0;
    double th=0;
    //南北半球标识
    switch(coorflag)
    {
    case Pos_BLH:
        //转换BLH为XYH 全部存储
        tlongitude=long_e/*/MYPI*180*/;  //弧度转成度分秒°
        tlatitude=lati_n/*/MYPI*180*/;
        //        tlongitude=lati_n/*/MYPI*180*/;  //弧度转成度分秒°
        //        tlatitude=long_e/*/MYPI*180*/;
        // int tgz=l_y/1000000;
        if(fabs(tlatitude)>90||fabs(tlongitude)>180)
        {
            //myPosition.posValidflag=false;
            return -1;  //经纬度坐标超界
        }
        QMyCoorConverse::UTMLaLotoXy(tlongitude,tlatitude,tfn,tnorth,teast);
        if(std::abs(qRound(teast)%1000000-500000)>335000)
        {
            //myPosition.posValidflag=false;
            return -2;  //东向超界
        } //东向超界
        QMyCoorConverse::BLH_TO_GZ(tlongitude,tlatitude,teastid,tnortthid);
        if(teastid>60||tnortthid<='B'||tnortthid>='Y')
        {
            //myPosition.posValidflag=false;
            return -3;  //代号不对超界
        }
        //全部正确时赋值
        myPosition.latitude_n=lati_n;
        myPosition.longitude_e=long_e;
        myPosition.latitude_dms=D2Dms(lati_n);
        myPosition.longitude_dms=D2Dms(long_e);

        myPosition.coorflag=coorflag;
        myPosition.eastzone=teastid;
        myPosition.northzone=tnortthid;
        myPosition.north_X=tnorth;
        myPosition.east_Y=teast;
        myPosition.h=h;
        myPosition.posValidflag=true;
        return 1;
        break;
    case Pos_XYH:
        //转换XYH为BLH 全部存储
        teastid = static_cast<unsigned short>(qRound(long_e) / 1000000);
        if(teastid>60)
        {
            qDebug()<<"myPosition.posValidflag=false";
            //myPosition.posValidflag=false;
            return -3;  //代号不对超界
        }
        if(abs((qRound(long_e)%1000000-500000))>335000)
        {
            qDebug()<<"myPosition.posValidflag=false";
            //myPosition.posValidflag=false;
            return -2;  //y值超界
        }
        XYtoLonLat(lati_n,long_e,h,0,0,hemisphereflag,tlon,tlat,th);
        tlatitude=dDuFenMiao_dDu(tlat);
        tlongitude=dDuFenMiao_dDu(tlon);
        if(fabs(tlatitude)>90||fabs(tlongitude)>180)
        {
            //myPosition.posValidflag=false;
            return -1;  //经纬度坐标超界
        }
        BLH_TO_GZ(tlongitude,tlatitude,teastid,tnortthid);
        if(tnortthid<='B'||tnortthid>='Y'||teastid!=(qRound(long_e)/1000000))
        {
            //myPosition.posValidflag=false;
            return -3;  //代号不对超界
        }
        //全部正确时赋值
        myPosition.h=h;
        myPosition.longitude_e=tlongitude ;//qRound(tlon*1000000);
        myPosition.latitude_n=tlatitude;//qRound(tlat*1000000);
        myPosition.latitude_dms=tlat;
        myPosition.longitude_dms=tlon;
        myPosition.east_Y=long_e;
        myPosition.north_X=lati_n;
        myPosition.eastzone=teastid;
        myPosition.northzone=tnortthid;
        myPosition.posValidflag=true;
        return 1;
        break ;
    default:
        return -4;
        break;
    }
}
// [ADD THIS IMPLEMENTATION TO qmycoorconverse.cpp]

void QMyCoorConverse::BLCoorToXYHLatCoor(const IBLHCoorData &input, XYH_LAT_COOR &output)
{
    // 1. Initialize Height
    output.h = input.h;

    // 2. Convert Integer DMS format (10^7 scaled) to Decimal Degrees (Double)
    // using the existing helper function Dms2D
    output.lat = Dms2D(input.lat);
    output.lon = Dms2D(input.lon);

    // 3. Determine Hemisphere (Type)
    // Bit0: 0 = North (Lat >= 0), 1 = South (Lat < 0)
    if (output.lat >= 0) {
        output.type = 0;
    } else {
        output.type = 1;
    }

    // 4. Convert Lat/Lon to Projected Coordinates (UTM/XY)
    double dX_Northing = 0.0;
    double dY_Easting = 0.0;
    double dFN = 0.0; // False Northing initialization

    // Note: Your existing UTMLaLotoXy automatically handles the FN (0 vs 10000000)
    // based on whether Lat is positive or negative.
    UTMLaLotoXy(output.lon, output.lat, dFN, dX_Northing, dY_Easting);

    // 5. Store result in output struct
    // Note: The struct uses uint, ensuring we cast correctly.
    // UTM coordinates are usually positive.
    output.x = static_cast<unsigned int>(dX_Northing);
    output.y = static_cast<unsigned int>(dY_Easting);
}
/**
 * @brief 将输入的投影坐标数据(IXYHCoorData)转换为详细坐标数据(XYH_LAT_COOR)
 * @param input  输入的投影坐标(x, y, h)
 * @param output 输出的混合坐标结构体(包含经纬度)
 */
void QMyCoorConverse::convertIXYHToXYHLatCoor(const IXYHCoorData &input, XYH_LAT_COOR &output)
{
    // 1. 处理高度 (注意：IXYHCoorData 的 h 是 short，XYH_LAT_COOR 的 h 也是 short)
    output.h = input.h;

    // 2. 准备中间变量，用于存储投影转换结果
    double tempLonDms = 0.0; // 经度 (度.分秒格式)
    double tempLatDms = 0.0; // 纬度 (度.分秒格式)
    double tempZh = 0.0;

    // 3. 执行投影反转 (坐标 -> 经纬度)
    // 根据 IXYHCoorData 的 type 字段判断半球（Bit0: 0北 1南）
    bool isSouthHemisphere = (input.type & 0x01);

    // 调用现有函数: 椭球选WGS84(0), 投影选UTM(0)
    // 注意：input.x 对应 Northing, input.y 对应 Easting
    XYtoLonLat(static_cast<double>(input.x),
               static_cast<double>(input.y),
               static_cast<double>(input.h),
               0, 0, isSouthHemisphere,
               tempLonDms, tempLatDms, tempZh);

    // 4. 将“度.分分秒秒”格式转换为“十进制度”格式
    output.lat = dDuFenMiao_dDu(tempLatDms);
    output.lon = dDuFenMiao_dDu(tempLonDms);

    // 5. 填充剩余的投影坐标和类型标识
    output.type = input.type;
    output.x = input.x;
    output.y = input.y;
}
//经纬度转度分分秒秒小数秒*10^6
double QMyCoorConverse::D2Dms(double d_data){
    int d = (int)d_data;
    int m = (int)((d_data-d)*60);
    double s = (((d_data-d)*60-m)*60);
    //qDebug()<<"d f m:"<<d<<m<<s;
    //double dms=d*(pow(10,6))+m*10000+s*100;
    double dms=d+m/100.0+s/10000.0;
    return  dms;
}
//度分分秒秒小数秒 转 度
double QMyCoorConverse::Dms2D(int dms_data){

    int d,m;
    double s;
    d=floor(dms_data/10000000.0);
    m=floor((dms_data%10000000)/100000.0);
    s=(dms_data%100000)/1000.0;
    qDebug()<<"d f m:"<<d<<m<<s;
    double d_0=d+m/60.0+s/3600.0;
    //qDebug()<<"d_0:"<<d_0;
    return  d_0;
}
double QMyCoorConverse::dmsDoubleToDecimal(double dms)
{
    // 1. 度
    int deg = static_cast<int>(dms);
    // 2. 得到分.秒
    double frac = dms - deg;           // 例如：0.3343
    double minSec = frac * 100.0;      // 33.43
    // 3. 分
    int minutes = static_cast<int>(minSec);  // 33
    // 4. 秒（含小数）
    double seconds = (minSec - minutes) * 100.0;  // 43.0
    // 5. 转十进制定理
    return deg + minutes / 60.0 + seconds / 3600.0;
}
QDateTime QMyCoorConverse::convertToOffsetZone(const QDateTime &dateTime, int offsetHours)
{
    //计算偏移秒数
    qint64 offsetSecs = static_cast<qint64>(offsetHours) * 3600;
    //在 UTC 基础上加上偏移秒数
    QDateTime result = dateTime.addSecs(offsetSecs);
    return result;
}
bool QMyCoorConverse::isXYHValid(double xNorthing, double yEasting, double h)
{
    // 0) 基础健壮性：NaN/Inf
    if (!std::isfinite(xNorthing) || !std::isfinite(yEasting) || !std::isfinite(h)) {

        return false;
    }
    // 1) 带号检查：zone = round(y)/1e6，应在 1..60（与你 coorConvert(Pos_XYH) 一致）
    const qint64 yRound = static_cast<qint64>(qRound64(yEasting));
    const int zone = static_cast<int>(yRound / 1000000LL);
    if (zone < 1 || zone > 60) {

        return false;
    }
    // 2) 带内偏移检查：abs( (round(y)%1e6) - 500000 ) <= 335000（与你现有逻辑一致）
    qint64 inZone = yRound % 1000000LL;
    if (inZone < 0) inZone += 1000000LL; // 保证为正
    const qint64 offset = inZone - 500000LL;
    if (std::llabs(offset) > 335000LL) {

        return false;
    }
    // 3) X(Northing) 粗略范围（保守，避免误判；可按业务收紧）
    if (xNorthing < -1000000.0 || xNorthing > 11000000.0) {

        return false;
    }
    // 4) H 高程范围（按工程常用给个宽松阈值；你也可自定义）
    if (h < -2000.0 || h > 20000.0) {

        return false;
    }
    return true;
}


void QMyCoorConverse::UTC2BeijingTime(const GPS_Time_Plus &gps_Time, GPS_Time_Plus &gpsTime_plus)
{
    // 1. 构造一个 QDateTime 对象，并明确指定它是 UTC 时间
    QDate date(gps_Time.year, gps_Time.month, gps_Time.day);
    QTime time(gps_Time.hour, gps_Time.min, gps_Time.sec, gps_Time.ms);
    QDateTime utcDateTime(date, time, Qt::UTC);
    // 2. 将 UTC 时间转换为北京时间 (UTC+8)
    qint64 offsetSecs = 8 * 3600; // +8 小时
    QDateTime beijingDateTime = utcDateTime.addSecs(offsetSecs);
    // 3. 将转换后的 QDateTime 对象的组件回填到 gpsTime_plus 结构体
    QDate beijingDate = beijingDateTime.date();
    QTime beijingTime = beijingDateTime.time();

    gpsTime_plus.year = beijingDate.year();
    gpsTime_plus.month = beijingDate.month();
    gpsTime_plus.day = beijingDate.day();

    gpsTime_plus.hour = beijingTime.hour();
    gpsTime_plus.min = beijingTime.minute();
    gpsTime_plus.sec = beijingTime.second();
    gpsTime_plus.ms = beijingTime.msec();
}
double QMyCoorConverse::TwoPointDistance_basic(double x1, double y1, double h1, double x2, double y2, double h2)
{
    double dx = x1 - x2;
    double dy = y1 - y2;
    double dh = h1 - h2;
    return std::sqrt(dx * dx + dy * dy + dh * dh);
}

bool QMyCoorConverse::TwoPointDistanceCompute(double x1, double y1, double h1,
                                              double x2, double y2, double h2,
                                              double &distance, double &angle,
                                              double &Gdj)
{
    // 优化2：提取常量，减少重复除法运算
    const double MIL = GlobleData::myCircle();
    const double RAD_TO_MIL = MIL / (2.0 * M_PI);

    double dx = x2 - x1;
    double dy = y2 - y1;
    double dh = h2 - h1;

    // 1. 计算水平距离 (用于高低角计算)
    double distHoriz = std::sqrt(dx * dx + dy * dy);

    // 2. 计算空间距离
    // 可以复用 distHoriz 避免重复开方运算: distance = sqrt(distHoriz^2 + dh^2)
    // 但为了保持精度和逻辑复用，调用 basic 也没问题，只要 basic 是内联的。
    distance = std::sqrt(distHoriz * distHoriz + dh * dh);

    // 3. 计算方位角 (Angle)
    // 优化3：使用 atan2 直接替代所有手动判断象限的 if-else 逻辑
    // atan2 返回值范围是 (-PI, PI]
    double angleRad = std::atan2(dy, dx);

    // 转换单位并归一化
    angle = angleRad * RAD_TO_MIL;
    if (angle < 0) {
        angle += MIL;
    }

    // 4. 计算高低角 (Gdj)
    // 优化4：防止水平距离为0导致除以0异常 (即两点垂直重合时)
    if (distHoriz < 1e-9) { // 极小值判断
        if (dh > 0) Gdj = MIL / 4.0;       // 垂直向上 90度
        else if (dh < 0) Gdj = -MIL / 4.0; // 垂直向下 -90度
        else Gdj = 0;
    } else {
        // 原逻辑是 atan(dh / dist)，这里不需要除以 PI 再除以 2，直接乘转化系数即可
        // (atan(x) / (2*PI)) * MIL  ==> atan(x) * (MIL / 2PI)
        Gdj = std::atan(dh / distHoriz) * RAD_TO_MIL;
    }

    return true;
}

