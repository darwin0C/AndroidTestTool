#ifndef QMYCOORCONVERSE_H
#define QMYCOORCONVERSE_H

#include <QObject>
#include "math.h"
#include "data.h"
#include "global.h"
#include "PublicInterface.h"
#include "globlecommon_global.h"
#define MYPI (2*asin(1.0))

class GLOBLECOMMON_EXPORT QMyCoorConverse : public QObject
{
    Q_OBJECT

    static double TwoPointDistance_basic(double x1, double y1, double h1, double x2, double y2, double h2);
public:
    QMyCoorConverse(QObject *parent=0);
    ~QMyCoorConverse();
    //弧度转度分秒
    static double RadtoDfm(double Rad);
    //度分秒转度
    static double dDuFenMiao_dDu( double dData );
    static void   GossXYtoLaLo(double a,double b,double FN,double x,double y,double &B,double &L);
    static void   UTMXYtoLaLo(double a,double b,double fn,double x,double y,double &lat,double &lon);
    static void   XYtoLonLat(double Northing,double Easting,double lh,unsigned char oveltype,unsigned char projtype,bool hemisphere,
                             double &lon,double &lat,double &zh);

    static void BLH_TO_GZ( double L, double B, unsigned short &Gz, unsigned char &ASII);

    static void UTMLaLotoXy(double Long,double Lat, double FN, double &UTMNorthing, double &UTMEasting);
    //增加一个BED转换成XYH的
    static void changeBEDToXYH(double B,double E,ushort D,double &x,double &y,double &h);


    static void CoorConvertDevice2XY(double dBy, double dEy, double dDy, double dAL, double dST, double dFI, double *dX, double *dY, double *dH);
    static void CoorRoll(double *dY, double *dX, double dAngle);
    static double Protect_atan2(double y, double x);
    static void Ground_aix_to_Guide_vol(double b, double e, double d, double n, double r, double p, double *az, double *el);
    static void Ground_aix_XYH_to_Guide_vol(double dx, double dy, double dh, double n, double r, double p, double *az, double *el);
    static int coorConvert(Position_Unique &myPosition, double b_x, double l_y, double h, unsigned char coorflag, unsigned char hemisphereflag);
    static double D2Dms(double d_data);
    static double Dms2D(int dms_data);
    static void UTC2BeijingTime(const GPS_Time_Plus &gps_Time, GPS_Time_Plus &gpsTime_plus);
    static bool TwoPointDistanceCompute(double x1, double y1, double h1, double x2, double y2, double h2, double &distance, double &angle, double &Gdj);
    static double dmsDoubleToDecimal(double dms);

    static QDateTime convertToOffsetZone(const QDateTime &dateTime, int offsetHours);
    static void BLCoorToXYHLatCoor(const IBLHCoorData &input, XYH_LAT_COOR &output);
    static void convertIXYHToXYHLatCoor(const IXYHCoorData &input, XYH_LAT_COOR &output);
    static bool isXYHValid(double xNorthing, double yEasting, double h);
};

#endif // QMYCOORCONVERSE_H
