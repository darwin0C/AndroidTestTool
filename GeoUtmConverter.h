#ifndef GEOUTMCONVERTER_H
#define GEOUTMCONVERTER_H

#include <cmath>
#include "globlecommon_global.h"
// 坐标转换类：UTM / Geo / ECEF 以及目标坐标计算
class GLOBLECOMMON_EXPORT GeoUtmConverter
{
public:
    // 大地坐标（经纬度 + 椭球高）
    struct GeoCoord
    {
        double latDeg;   // 纬度（度）
        double lonDeg;   // 经度（度）
        double height;   // 椭球高 h（米）
    };

    // UTM 坐标
    //
    // XE = zone * 1e6 + easting
    //   zone    : 1 ~ 60
    //   easting : 0 ~ 1e6 之间（实际约 166000~833000）
    //
    struct UtmCoord
    {
        double XE;              // 带号与东坐标合并后的值
        bool   northHemisphere; // 是否北半球
        double northing;        // 北坐标 N（米）
        double height;          // 椭球高 h（米）
    };

    // 目标坐标计算结果（UTM接口用）
    struct TargetResult
    {
        GeoCoord geo;   // 目标大地坐标
        UtmCoord utm;   // 目标 UTM 坐标（自动跨带）
    };

public:
    // 默认构造：使用 WGS-84 椭球
    GeoUtmConverter();

    // 自定义椭球（例如 CGCS2000，可传入 a, f）
    GeoUtmConverter(double a, double f, double k0 = 0.9996);

    // UTM -> 大地坐标
    GeoCoord utmToGeo(const UtmCoord& utm) const;

    // 大地坐标 -> UTM（自动选 UTM 带号，可跨带）
    UtmCoord geoToUtm(const GeoCoord& geo, int utmZone = -1) const;

    // 大地坐标 -> ECEF
    void geoToECEF(const GeoCoord& geo,
                   double& X, double& Y, double& Z) const;

    // ECEF -> 大地坐标
    GeoCoord ecefToGeo(double X, double Y, double Z) const;


     //检查输入的 UTM 坐标是否跨带，如果跨带，则计算并返回新的 UTM 坐标。
    UtmCoord checkAndCorrectCrossedZone(const UtmCoord& selfUtm) const;


    // 已知己方经纬度 + 斜距 + 方位角(真北) + 高低角，计算目标经纬度
    //
    // 约定：
    //   range_m       : 斜距（米）
    //   azimuth_rad   : 方位角（弧度），以“正北”为 0，顺时针为正（东 π/2，南 π，西 3π/2）
    //   elevation_rad : 高低角（弧度），仰角为正，俯角为负
    //
    GeoCoord computeTargetFromGeo(const GeoCoord& selfGeo,
                                  double range_m,
                                  double azimuth_rad,
                                  double elevation_rad) const;

    // 网格北方位角 → 真北方位角（弧度）
    //
    // 输入：
    //   selfUtm      : 己方 UTM 坐标
    //   az_grid_rad  : 相对于 Grid North 的方位角（弧度）
    //
    // 输出：
    //   返回相对于 True North 的方位角（弧度，归一化到 0 ~ 2π）
    //
    double gridAzToTrueAzRad(const UtmCoord& selfUtm,
                             double az_grid_rad) const;

    // --------- XE 编码/解码工具 ---------
    // XE = zone * 1e6 + easting
    static double makeXE(int zone, double easting);
    static int    extractZone(double XE);
    static double extractEasting(double XE);


private:
    // 椭球参数
    double m_a;   // 长半轴
    double m_f;   // 扁率
    double m_e2;  // 第一偏心率平方
    double m_k0;  // 投影尺度因子（UTM 0.9996）

    // 小工具：角度与弧度互转
    static double deg2rad(double d);
    static double rad2deg(double r);
};



#endif // GEOUTMCONVERTER_H
