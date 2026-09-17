import { get, post } from '@/request'
import type { LoginUserVO, PageResult, UserVO } from '@/types/user'

export function userRegister(payload: {
  userAccount: string
  userPassword: string
  checkPassword: string
}) {
  return post<number>('/user/register', payload)
}

export function userLogin(payload: { userAccount: string; userPassword: string }) {
  return post<LoginUserVO>('/user/login', payload)
}

export function getLoginUser() {
  return get<LoginUserVO>('/user/get/login')
}

export function userLogout() {
  return post<boolean>('/user/logout')
}

export function listUserVOByPage(payload: {
  current: number
  pageSize: number
  userAccount?: string
  userName?: string
}) {
  return post<PageResult<UserVO>>('/user/list/page/vo', payload)
}

export function addUser(payload: {
  userAccount: string
  userPassword: string
  userName?: string
  userRole?: string
}) {
  return post<number>('/user/add', payload)
}

export function deleteUser(id: number) {
  return post<boolean>('/user/delete', { id })
}
