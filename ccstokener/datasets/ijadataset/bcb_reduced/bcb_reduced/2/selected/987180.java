package cn.ziroom.house.service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import net.sf.jasperreports.engine.JRException;
import org.apache.tools.zip.ZipOutputStream;
import cn.common.manager.SessionManager;
import cn.common.resource.ResourceManager;
import cn.common.util.CompressionUtil;
import cn.common.util.GeneralUtils;
import cn.common.util.JasperreportsUtils;
import cn.common.util.StringUtils;
import cn.ziroom.contract.domain.RoomPictures;
import cn.ziroom.house.domain.House;
import cn.ziroom.house.domain.Room;
import cn.ziroom.house.mapper.HouseAndRoomMapper;
import cn.ziroom.system.domain.User;

/**
 * 房屋业务处理类
 * 
 * @author Administrator
 * 
 */
public class HouseService {

    private HouseAndRoomMapper houseAndRoomMapper;

    private DataSource dataSource;

    /**
	 * 
	 * @param type
	 * @param param
	 * @return
	 */
    public String getDeptOrPerson(String type, String param) {
        List<User> list = new ArrayList<User>();
        User u = new User();
        if (type.equals("first")) {
            u.setDeptCode("");
            u.setDepartName("请选择业务组");
            list.add(u);
            User user = new User();
            user.setDeptPath(param);
            if (param != null && !param.equals("")) {
                list.addAll(houseAndRoomMapper.selectByFirst(user));
            }
        } else if (type.equals("second")) {
            u.setStaffid("");
            u.setStaffName("请选择主管");
            list.add(u);
            User user = new User();
            user.setDeptPath(param);
            list.addAll(houseAndRoomMapper.selectBySecond(user));
        } else if (type.equals("third")) {
            u.setStaffid("");
            u.setStaffName("请选择管家");
            list.add(u);
            User user = new User();
            user.setDeptPath(param);
            list.addAll(houseAndRoomMapper.selectByThird(user));
        }
        return GeneralUtils.easyUI(list);
    }

    /**
	 * 搜索房屋业务处理方法
	 * 
	 * @param house
	 * @return
	 */
    public String searchHouse(House house) {
        User user = (User) SessionManager.getAttr(SessionManager.USER_KEY);
        if (user.getSetidJobcode() != null && user.getSetidJobcode().equals("专员")) {
            house.setStaffid(user.getStaffid());
        } else if (user.getSetidJobcode() != null && user.getSetidJobcode().equals("主管")) {
            house.setDepartName(user.getDeptCode());
        } else {
            house.setDepartName(user.getDeptCode());
        }
        if (house.getR1() != null && !house.getR1().equals("")) {
            User u = new User();
            u.setStaffid(house.getR1());
            u = houseAndRoomMapper.selectByStaff(u);
            house.setR1(u.getDeptCode());
        }
        return GeneralUtils.easyUI(houseAndRoomMapper.countHouse(house), houseAndRoomMapper.searchHouseList(house));
    }

    /**
	 * 根据房源编号查找房源编号的房间列表业务处理方法
	 * 
	 * @param code
	 * @return
	 */
    public List<Room> searchRoomByHouseCode(Integer code) {
        return houseAndRoomMapper.searchRoomByHouseCode(code);
    }

    /**
	 * 房屋发布页面请求处理方法
	 * 
	 * @param house
	 * @return
	 */
    public House housePubForm(House house) {
        house = houseAndRoomMapper.selectHouseByCode(house.getSysHouseId());
        return house;
    }

    /**
	 * 房屋发布业务处理方法
	 * 
	 * @param houseCode
	 */
    public void pubHouse(House house) {
        House h = houseAndRoomMapper.selectHouseByCode(house.getSysHouseId());
        if (h != null) {
            Room r = new Room();
            r.setSysHouseId(h.getSysHouseId());
            if (h.getIsShow().equals("N")) {
                h.setIsShow("Y");
                h.setCkd("1");
                h.setCheckInTime(house.getCheckInTime());
                r.setIsShow("Y");
                r.setCkd("1");
                r.setCheckInTime(house.getCheckInTime());
            } else if (h.getIsShow().equals("Y")) {
                h.setIsShow("N");
                h.setCkd("1");
                h.setCheckInTime(null);
                r.setIsShow("N");
                r.setIsPromotions("N");
                r.setIsTop("N");
                r.setCkd("1");
                r.setCheckInTime(null);
            }
            houseAndRoomMapper.updateRoomByHouseCode(r);
            houseAndRoomMapper.updateHouse(h);
        }
    }

    /**
	 * 房间发布页面请求处理方法
	 * 
	 * @param house
	 * @return
	 */
    public Room roomPubForm(Room room) {
        room = houseAndRoomMapper.selectRoomByCode(room.getSysRoomId());
        return room;
    }

    /**
	 * 房间发布功能
	 * 
	 * @param roomCode
	 */
    public void pubRoom(Room r) {
        Room room = houseAndRoomMapper.selectRoomByCode(r.getSysRoomId());
        if (room != null) {
            if (room.getIsShow().equals("Y")) {
                room.setIsShow("N");
                room.setIsPromotions("N");
                room.setIsTop("N");
                room.setCkd("1");
                room.setCheckInTime(null);
            } else if (room.getIsShow().equals("N")) {
                room.setIsShow("Y");
                room.setCkd("1");
                if (r.getCheckInTime() != null) {
                    room.setCheckInTime(r.getCheckInTime());
                }
            }
        }
        houseAndRoomMapper.updateRoom(room);
        int countRoom = houseAndRoomMapper.countRoom(room.getSysHouseId());
        int countPubRoom = houseAndRoomMapper.countPubRoom(room);
        if (countRoom == countPubRoom) {
            House h = new House();
            h.setSysHouseId(room.getSysHouseId());
            h.setIsShow(room.getIsShow());
            houseAndRoomMapper.updateHouse(h);
        }
    }

    /**
	 * 房间推荐
	 * 
	 * @param roomCode
	 * @return
	 */
    public String recommendRoom(Integer roomCode) {
        Room room = houseAndRoomMapper.selectRoomByCode(roomCode);
        String isTop = room.getIsTop();
        if (isTop.equals("N")) {
            int topNum = houseAndRoomMapper.countTopRoomNum(room.getSysHouseId());
            if (topNum != 0) {
                return "一个房屋只能推荐一个房源！";
            }
            User user = (User) SessionManager.getAttr(SessionManager.USER_KEY);
            if (user.getSetidJobcode() != null && (user.getSetidJobcode().equals("主管") || user.getSetidJobcode().equals("总监") || user.getSetidJobcode().equals("副总经理"))) {
                int num = houseAndRoomMapper.countTop(user.getStaffid());
                if (num >= 3) {
                    return "对多只能推荐3个房间！";
                }
                room.setIsTop("Y");
            }
        } else {
            room.setIsTop("N");
        }
        houseAndRoomMapper.updateRoom(room);
        return "success";
    }

    /**
	 * 房间优先展示
	 * 
	 * @param roomCode
	 * @return
	 */
    public String priorityRoom(Integer roomCode) {
        Room room = houseAndRoomMapper.selectRoomByCode(roomCode);
        String isPromotions = room.getIsPromotions();
        if (isPromotions.equals("N")) {
            int promotionsNum = houseAndRoomMapper.countPromotionsRoomNum(room.getSysHouseId());
            if (promotionsNum != 0) {
                return "一个房屋只能有一个优先展示房间！";
            }
            User user = (User) SessionManager.getAttr(SessionManager.USER_KEY);
            if (user.getSetidJobcode() != null && (user.getSetidJobcode().equals("主管") || user.getSetidJobcode().equals("总监") || user.getSetidJobcode().equals("副总经理"))) {
                int num = houseAndRoomMapper.countPromotions(user.getStaffid());
                if (num >= 3) {
                    return "对多只能优先展示3个房间！";
                }
                room.setIsPromotions("Y");
            }
        } else {
            room.setIsPromotions("N");
        }
        houseAndRoomMapper.updateRoom(room);
        return "success";
    }

    /**
	 * 保存房屋
	 * 
	 * @param house
	 */
    public void saveHouse(House house) {
        houseAndRoomMapper.updateHouse(house);
    }

    /**
	 * 保存房间
	 * 
	 * @param house
	 */
    public void saveRoom(Room room) {
        houseAndRoomMapper.updateRoom(room);
    }

    /**
	 * 生成房源包
	 * 
	 * @param house
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws JRException
	 */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map<String, String> createHousePackage(Room room) throws IOException, JRException, SQLException {
        Map<String, String> housePackage = new HashMap<String, String>();
        System.out.println("读取房源包路径");
        URL s = HouseService.class.getClassLoader().getResource("");
        String tempDir = s.getPath().substring(0, s.getPath().lastIndexOf("WEB-INF")) + "//housePackage/";
        Room r = houseAndRoomMapper.selectByRoomId(room.getRoomId());
        House h = houseAndRoomMapper.selectHouseByCode(r.getSysHouseId());
        String fileName = h.getHouseCode() + r.getRoomName() + ".zip";
        String file = tempDir + "//" + fileName;
        housePackage.put("fileName", fileName);
        housePackage.put("downFilePath", file);
        FileOutputStream fos = new FileOutputStream(file);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
        zos.setEncoding("GBK");
        String imageUrl = ResourceManager.getString("image.url");
        List<RoomPictures> rps = houseAndRoomMapper.selectRoomPicturesByRoomId(r.getSysRoomId());
        int i = 0;
        long start = System.currentTimeMillis();
        for (RoomPictures rp : rps) {
            String path = rp.getImagepath();
            if (path != null && !path.equals("")) {
                int _a = path.lastIndexOf("/");
                path = path.substring(0, _a) + "//v480x360_" + path.substring(_a + 1);
                URL url = new URL(imageUrl + "//" + path);
                InputStream is = url.openStream();
                String _fileName = "室内图片/" + (i++) + ".jpg";
                CompressionUtil.compressFile(zos, is, _fileName);
                is.close();
            }
        }
        List<RoomPictures> hps = houseAndRoomMapper.selectRoomPicturesByHouseId(r.getSysHouseId());
        i = 0;
        for (RoomPictures rp : hps) {
            String path = rp.getImagepath();
            if (path != null && !path.equals("")) {
                int _a = path.lastIndexOf("/");
                path = path.substring(0, _a) + "//v480x360_" + path.substring(_a + 1);
                URL url = new URL(imageUrl + "//" + path);
                InputStream is = url.openStream();
                String _fileName = "公共区域图片/" + (i++) + ".jpg";
                CompressionUtil.compressFile(zos, is, _fileName);
                is.close();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("图片打包时间：" + (end - start) / 1000);
        Map map = new HashMap();
        map.put("sysRoomId", r.getSysRoomId());
        map.put("itemType", "jj4");
        map.put("sysHouseId", r.getSysHouseId());
        List<String> _c1 = houseAndRoomMapper.selectItemByRoom(map);
        List<String> _c2 = houseAndRoomMapper.selectItemByPub(map);
        map.put("itemType", "jd");
        List<String> _c3 = houseAndRoomMapper.selectItemByRoom(map);
        List<String> _c4 = houseAndRoomMapper.selectItemByPub(map);
        map.put("itemType", "jj1");
        List<String> _c5 = houseAndRoomMapper.selectItemByRoom(map);
        List<String> _c6 = houseAndRoomMapper.selectItemByPub(map);
        System.out.println("读取jasper文件");
        StringBuilder sb = new StringBuilder();
        List<Map> rooms = houseAndRoomMapper.selectOtherRoom(map);
        int count = 0;
        for (int j = 0; j < rooms.size(); j++) {
            Map rm = rooms.get(j);
            String rentstatus = rm.get("rentstatus") != null ? rm.get("rentstatus").toString() : "";
            String position = rm.get("roomposition") != null ? rm.get("roomposition").toString() : "";
            if (rentstatus.equals("ycz")) {
                String sex = rm.get("sex") != null ? rm.get("sex").toString() : "";
                String professional = rm.get("professional") != null ? rm.get("professional").toString() : "";
                String code = rm.get("code") != null ? rm.get("code").toString() : "";
                ;
                sb.append(!position.equals("") ? (position + "卧" + code + "住着一位") : "");
                sb.append(!sex.equals("") ? sex + "性" : "");
                sb.append(!professional.equals("") ? professional : "");
                sb.append(";");
            } else {
                sb.append(!position.equals("") ? (position + "卧待租 ;") : "卧室待租; ");
                count++;
            }
        }
        if (count == rooms.size()) {
            sb = new StringBuilder();
        }
        URL u = this.getClass().getClassLoader().getResource("cn/ziroom/house/service/package.jasper");
        System.out.println("文件路径：" + u.getPath());
        String reportFile = u.getFile();
        Map parameter = new HashMap();
        parameter.put("roomId", room.getRoomId());
        parameter.put("c1", StringUtils.split(_c1, ","));
        parameter.put("c2", StringUtils.split(_c2, ","));
        parameter.put("c3", StringUtils.split(_c3, ","));
        parameter.put("c4", StringUtils.split(_c4, ","));
        parameter.put("c5", StringUtils.split(_c5, ","));
        parameter.put("c6", StringUtils.split(_c6, ","));
        parameter.put("c7", sb.toString());
        start = System.currentTimeMillis();
        System.out.println("填充报表");
        String filePath = JasperreportsUtils.docx(reportFile, parameter, dataSource.getConnection());
        File _file = new File(filePath);
        FileInputStream fis = new FileInputStream(_file);
        CompressionUtil.compressFile(zos, fis, h.getHouseCode() + r.getRoomName() + ".docx");
        fis.close();
        _file.delete();
        String docFilePath = JasperreportsUtils.doc(reportFile, parameter, dataSource.getConnection());
        File _docfile = new File(docFilePath);
        FileInputStream docfis = new FileInputStream(_docfile);
        CompressionUtil.compressFile(zos, docfis, h.getHouseCode() + r.getRoomName() + ".doc");
        fis.close();
        _docfile.delete();
        byte[] html = JasperreportsUtils.html(reportFile, parameter, dataSource.getConnection());
        InputStream is = new ByteArrayInputStream(html);
        CompressionUtil.compressFile(zos, is, h.getHouseCode() + r.getRoomName() + ".html");
        end = System.currentTimeMillis();
        System.out.println("报表生成时间:" + (end - start) / 1000);
        is.close();
        zos.close();
        fos.close();
        return housePackage;
    }

    public House houseView(Integer houseCode) {
        House house = houseAndRoomMapper.selectHouse(houseCode);
        return house;
    }

    public void setHouseAndRoomMapper(HouseAndRoomMapper houseAndRoomMapper) {
        this.houseAndRoomMapper = houseAndRoomMapper;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
