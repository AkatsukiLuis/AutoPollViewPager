# AutoPollViewPager 无限循环
实现思路：在列表的最前面插入最后一条数据，在列表末尾插入第一个数据，造成循环的假象；

假设有三条数据，分别编号1、2、3，我们再创建一个新的列表，长度为真实列表的长度+2，在最前面插入最后一条数据3，在最后面插入第一条数据1，新列表就变为3、1、2、3、1，当viewpager滑动到位置0时就通过setCurrentItem(int item,boolean smoothScroll)方法将页面切换到位置3，同理当滑动到位置4时，通过该方法将页面切换到位置1，这样给我们的感觉就是无限循环。
## adapter和ViewPager 都已经封装到AutoPollViewPager，用法和普通ViewPager一样。
