export interface LoginUserVO {
  id: number
  userAccount: string
  userName: string
  userAvatar?: string
  userProfile?: string
  userRole: string
  createTime?: string
}

export interface UserVO {
  id: number
  userAccount: string
  userName: string
  userAvatar?: string
  userProfile?: string
  userRole: string
  createTime?: string
  updateTime?: string
}

export interface PageResult<T> {
  records: T[]
  pageNumber: number
  pageSize: number
  totalRow: number
  totalPage: number
}
